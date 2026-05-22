package com.rdc.admin.client;

import com.rdc.admin.dto.ai.FastApiExecuteRequest;
import com.rdc.admin.dto.ai.FastApiExecuteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FastApiClient {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${service.fastapi.url}")
    private String fastApiBaseUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @Value("${ai.upscale.url:}")
    private String aiUpscaleUrl;

    public FastApiExecuteResponse execute(FastApiExecuteRequest request) {
        if (isSmartUpscaleRequest(request)) {
            return executeSmartUpscale(request);
        }

        String url = fastApiBaseUrl.replaceAll("/+$", "") + "/internal/ai/execute";
        log.info("Routing AI tool {} to {}", request.getToolName(), url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        HttpEntity<FastApiExecuteRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );

        String responseBody = response.getBody();
        if (requestedOutputCount(request) > 1) {
            log.warn(
                    "FastAPI raw multi-image response for tool {} with requested count {}: {}",
                    request.getToolName(),
                    requestedOutputCount(request),
                    responseBody
            );
        }

        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(responseBody, Map.class);
            return FastApiExecuteResponse.fromPayload(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse FastAPI response body", exception);
        }
    }

    private FastApiExecuteResponse executeSmartUpscale(FastApiExecuteRequest request) {
        String inputUrl = request.getInputUrl();
        if (inputUrl == null || inputUrl.isBlank()) {
            throw new IllegalStateException("Smart upscale requires inputUrl");
        }
        if (request.getAuthToken() == null || request.getAuthToken().isBlank()) {
            throw new IllegalStateException("Smart upscale requires user auth token");
        }

        String url = resolveSmartUpscaleUrl();
        log.info("Routing smart upscale to {}", url);

        ResponseEntity<byte[]> fileResponse = restTemplate.exchange(
                inputUrl,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                byte[].class
        );

        byte[] fileBytes = fileResponse.getBody();
        if (fileBytes == null || fileBytes.length == 0) {
            throw new IllegalStateException("Unable to download input image for smart upscale");
        }

        String filename = resolveFilename(inputUrl);
        MediaType fileMediaType = fileResponse.getHeaders().getContentType();

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(fileMediaType != null ? fileMediaType : MediaType.APPLICATION_OCTET_STREAM);

        ByteArrayResource fileResource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(fileResource, fileHeaders));

        if (request.getParams() != null) {
            request.getParams().forEach((key, value) -> {
                if (value != null) {
                    body.add(key, String.valueOf(value));
                }
            });
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(request.getAuthToken());

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
        } catch (HttpClientErrorException.NotFound exception) {
            log.error("Smart upscale endpoint not found at {}", url);
            FastApiExecuteResponse errorResponse = new FastApiExecuteResponse();
            errorResponse.setSuccess(false);
            errorResponse.setMessage(
                    "Smart upscale endpoint not found at " + url
                            + ". Configure AI_UPSCALE_URL with the actual FastAPI smart-upscale route."
            );
            return errorResponse;
        }

        String responseBody = response.getBody();
        log.info("Smart upscale raw response: {}", responseBody);

        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(responseBody, Map.class);
            return FastApiExecuteResponse.fromPayload(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse smart upscale response body", exception);
        }
    }

    public String resolveUrl(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) {
            return null;
        }

        String trimmed = urlOrPath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        String baseUrl = fastApiBaseUrl.replaceAll("/+$", "");
        return trimmed.startsWith("/") ? baseUrl + trimmed : baseUrl + "/" + trimmed;
    }

    private int requestedOutputCount(FastApiExecuteRequest request) {
        if (request == null || request.getParams() == null || request.getParams().isEmpty()) {
            return 1;
        }

        Object value = request.getParams().get("num_images");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return 1;
            }
        }

        return 1;
    }

    private boolean isSmartUpscaleRequest(FastApiExecuteRequest request) {
        if (request == null || !"UPSCALE".equals(request.getToolName()) || request.getParams() == null) {
            return false;
        }

        String mode = firstStringParam(
                request.getParams(),
                "mode",
                "model",
                "upscaleModel",
                "upscale_model",
                "modelType",
                "model_type"
        );
        if (mode == null || mode.isBlank()) {
            return false;
        }

        String normalized = mode.trim().toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        return "smart".equals(normalized) || "smart_upscale".equals(normalized);
    }

    private String firstStringParam(Map<String, Object> params, String... keys) {
        for (String key : keys) {
            Object value = params.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String resolveFilename(String inputUrl) {
        try {
            String path = URI.create(inputUrl).getPath();
            if (path == null || path.isBlank()) {
                return "input-image";
            }

            int lastSlash = path.lastIndexOf('/');
            String filename = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
            return filename == null || filename.isBlank() ? "input-image" : filename;
        } catch (Exception exception) {
            return "input-image";
        }
    }

    private String resolveSmartUpscaleUrl() {
        if (aiUpscaleUrl != null && !aiUpscaleUrl.isBlank()) {
            return aiUpscaleUrl.trim();
        }

        return fastApiBaseUrl.replaceAll("/+$", "") + "/ai/upscale";
    }
}
