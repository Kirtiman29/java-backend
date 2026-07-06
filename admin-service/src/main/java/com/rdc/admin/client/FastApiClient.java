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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Value("${service.ai.asset.url:${service.asset.url}}")
    private String aiAssetBaseUrl;

    @Value("${ai.upscale.url:}")
    private String aiUpscaleUrl;

    public FastApiExecuteResponse execute(FastApiExecuteRequest request) {
        if (isSmartUpscaleRequest(request)) {
            return executeSmartUpscale(request);
        }

        String url = fastApiBaseUrl.replaceAll("/+$", "") + "/internal/ai/execute";
        log.info("Routing AI tool {} to {}", request.getToolName(), url);

        FastApiExecuteRequest forwardedRequest = FastApiExecuteRequest.builder()
                .requestId(request.getRequestId())
                .userId(request.getUserId())
                .toolName(request.getToolName())
                .inputUrl(resolveAiAssetUrl(request.getInputUrl()))
                .params(resolveAiAssetUrls(request.getParams()))
                .authToken(request.getAuthToken())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        HttpEntity<FastApiExecuteRequest> entity = new HttpEntity<>(forwardedRequest, headers);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
        } catch (RestClientException exception) {
            log.error("AI execution request to {} failed for tool {}: {}", url, request.getToolName(), exception.getMessage(), exception);
            return failureResponse("Unable to reach AI execution service.");
        }

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
            log.error("Unable to parse FastAPI response body for tool {}: {}", request.getToolName(), exception.getMessage(), exception);
            return failureResponse("Unable to parse AI execution response.");
        }
    }

    private FastApiExecuteResponse executeSmartUpscale(FastApiExecuteRequest request) {
        String inputUrl = resolveAiAssetUrl(request.getInputUrl());
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
            return failureResponse(
                    "Smart upscale endpoint not found at " + url
                            + ". Configure AI_UPSCALE_URL with the actual FastAPI smart-upscale route."
            );
        } catch (RestClientException exception) {
            log.error("Smart upscale request to {} failed: {}", url, exception.getMessage(), exception);
            return failureResponse("Unable to reach AI smart-upscale service.");
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
            log.error("Unable to parse smart upscale response body: {}", exception.getMessage(), exception);
            return failureResponse("Unable to parse AI smart-upscale response.");
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

    private String resolveAiAssetUrl(String inputUrl) {
        if (inputUrl == null || inputUrl.isBlank()) {
            return inputUrl;
        }

        String assetBase = aiAssetBaseUrl == null ? null : aiAssetBaseUrl.trim();
        if (assetBase == null || assetBase.isBlank()) {
            assetBase = null;
        }

        try {
            URI input = URI.create(inputUrl.trim());
            URI base = assetBase != null ? URI.create(assetBase) : null;
            URI reachableBase = resolveReachableAssetBase(base, input.getScheme());

            if (!input.isAbsolute()) {
                return reachableBase != null ? joinBaseAndPath(reachableBase, inputUrl.trim()) : inputUrl.trim();
            }

            if (!isLoopbackHost(input.getHost())) {
                return inputUrl.trim();
            }

            if (reachableBase == null) {
                return inputUrl.trim();
            }

            URI rebuilt = new URI(
                    reachableBase.getScheme(),
                    reachableBase.getUserInfo(),
                    reachableBase.getHost(),
                    reachableBase.getPort(),
                    input.getPath(),
                    input.getQuery(),
                    input.getFragment()
            );
            return rebuilt.toString();
        } catch (IllegalArgumentException | URISyntaxException exception) {
            log.warn("Unable to normalize AI asset URL '{}': {}", inputUrl, exception.getMessage());
            return inputUrl;
        }
    }

    private Map<String, Object> resolveAiAssetUrls(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return params;
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        params.forEach((key, value) -> normalized.put(key, resolveAiAssetValue(value)));
        return normalized;
    }

    private Object resolveAiAssetValue(Object value) {
        if (value instanceof String text) {
            return resolvePotentialAssetUrl(text);
        }

        if (value instanceof List<?> items) {
            return items.stream()
                    .map(this::resolveAiAssetValue)
                    .toList();
        }

        if (value instanceof Map<?, ?> mapValue) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            mapValue.forEach((key, nestedValue) -> normalized.put(String.valueOf(key), resolveAiAssetValue(nestedValue)));
            return normalized;
        }

        return value;
    }

    private String resolvePotentialAssetUrl(String value) {
        String trimmed = value.trim();
        if (trimmed.isBlank()) {
            return value;
        }

        try {
            URI uri = URI.create(trimmed);
            String path = uri.getPath();

            if ((path != null && path.contains("/api/assets/"))
                    && (!uri.isAbsolute() || isLoopbackHost(uri.getHost()))) {
                return resolveAiAssetUrl(trimmed);
            }
        } catch (IllegalArgumentException ignored) {
            // Non-URL strings are ordinary prompt/model params and should pass through unchanged.
        }

        return value;
    }

    private String joinBaseAndPath(URI base, String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return base.toString().replaceAll("/+$", "") + normalizedPath;
    }

    private URI resolveReachableAssetBase(URI configuredBase, String fallbackScheme) {
        if (configuredBase != null && !isLoopbackHost(configuredBase.getHost())) {
            return configuredBase;
        }

        try {
            String localHostAddress = InetAddress.getLocalHost().getHostAddress();
            if (localHostAddress == null || localHostAddress.isBlank()) {
                return configuredBase;
            }

            String scheme = configuredBase != null && configuredBase.getScheme() != null
                    ? configuredBase.getScheme()
                    : (fallbackScheme != null ? fallbackScheme : "http");

            int port = configuredBase != null ? configuredBase.getPort() : -1;
            String userInfo = configuredBase != null ? configuredBase.getUserInfo() : null;
            String path = configuredBase != null ? configuredBase.getPath() : null;

            return new URI(scheme, userInfo, localHostAddress, port, path, null, null);
        } catch (UnknownHostException | URISyntaxException exception) {
            log.warn("Unable to resolve a reachable AI asset base URL: {}", exception.getMessage());
            return configuredBase;
        }
    }

    private boolean isLoopbackHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }

        String normalized = host.trim().toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "0.0.0.0".equals(normalized)
                || "::1".equals(normalized);
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

    private FastApiExecuteResponse failureResponse(String message) {
        FastApiExecuteResponse errorResponse = new FastApiExecuteResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage(message);
        return errorResponse;
    }
}
