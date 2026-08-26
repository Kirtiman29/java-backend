package com.rdc.admin.client;

import com.rdc.admin.dto.ai.FastApiExecuteRequest;
import com.rdc.admin.dto.ai.FastApiExecuteResponse;
import com.rdc.admin.dto.ai.FastApiJobResponse;
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

import java.net.URI;
import java.net.URISyntaxException;
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

    @Value("${service.fastapi.public-url:${service.fastapi.url}}")
    private String fastApiPublicBaseUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @Value("${service.ai.asset.url:${service.asset.url}}")
    private String aiAssetBaseUrl;

    @Value("${service.asset.url:http://localhost:8090}")
    private String assetServiceBaseUrl;

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

    public FastApiJobResponse createJob(FastApiExecuteRequest request) {
        String url = fastApiBaseUrl.replaceAll("/+$", "") + "/internal/ai/jobs";
        log.info("Queueing AI tool {} at {}", request.getToolName(), url);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", request.getUserId());
        body.put("featureName", request.getToolName());
        body.put("inputUrl", resolveAiAssetUrl(request.getInputUrl()));
        body.put("params", buildQueuedJobParams(request));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            return parseJobResponse(response.getBody(), request.getToolName());
        } catch (RestClientException exception) {
            log.error("AI job queue request to {} failed for tool {}: {}", url, request.getToolName(), exception.getMessage(), exception);
            return FastApiJobResponse.failure("Unable to queue AI job.");
        }
    }

    public FastApiJobResponse getJobStatus(Long jobId) {
        if (jobId == null) {
            return FastApiJobResponse.failure("jobId is required.");
        }

        String url = fastApiBaseUrl.replaceAll("/+$", "") + "/internal/ai/jobs/" + jobId;
        log.debug("Fetching AI job status from {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalServiceKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            return parseJobResponse(response.getBody(), null);
        } catch (RestClientException exception) {
            log.error("AI job status request to {} failed: {}", url, exception.getMessage(), exception);
            return FastApiJobResponse.failure("Unable to fetch AI job status.");
        }
    }

    private FastApiJobResponse parseJobResponse(String responseBody, String fallbackToolName) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(responseBody, Map.class);
            FastApiJobResponse response = FastApiJobResponse.fromPayload(payload);
            if (response != null && (response.getFeatureName() == null || response.getFeatureName().isBlank())) {
                response.setFeatureName(fallbackToolName);
            }
            return response;
        } catch (Exception exception) {
            log.error("Unable to parse AI job response body: {}", exception.getMessage(), exception);
            return FastApiJobResponse.failure("Unable to parse AI job response.");
        }
    }

    private Map<String, Object> buildQueuedJobParams(FastApiExecuteRequest request) {
        Map<String, Object> params = resolveAiAssetUrls(request.getParams());
        if (!isSeamlessTool(request.getToolName())) {
            return params;
        }

        Map<String, Object> enriched = new LinkedHashMap<>();
        if (params != null) {
            enriched.putAll(params);
        }
        enriched.putIfAbsent("publicBaseUrl", fastApiPublicBaseUrl);
        return enriched;
    }

    private boolean isSeamlessTool(String toolName) {
        if (toolName == null) {
            return false;
        }

        String normalized = toolName.trim().toUpperCase(Locale.ROOT);
        return "SEAMLESS".equals(normalized)
                || "SEAMLESS_GENERATOR".equals(normalized)
                || "SEAMLESS_PATTERN".equals(normalized);
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
            URI reachableBase = resolveReachableAssetBase(base);

            if (!input.isAbsolute()) {
                return reachableBase != null ? joinBaseAndPath(reachableBase, inputUrl.trim()) : inputUrl.trim();
            }

            if (!isAssetDownloadUrl(input) && !isLocalOnlyHost(input.getHost())) {
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

            if (isAssetDownloadUrl(uri)) {
                return resolveAiAssetUrl(trimmed);
            }
        } catch (IllegalArgumentException ignored) {
            // Non-URL strings are ordinary prompt/model params and should pass through unchanged.
        }

        return value;
    }

    private boolean isAssetDownloadUrl(URI uri) {
        String path = uri.getPath();
        return path != null && path.contains("/api/assets/");
    }

    private String joinBaseAndPath(URI base, String path) {
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return base.toString().replaceAll("/+$", "") + normalizedPath;
    }

    private URI resolveReachableAssetBase(URI configuredBase) {
        if (isUsableAssetBase(configuredBase)) {
            return configuredBase;
        }

        URI fallbackBase = parseAssetBase(assetServiceBaseUrl);
        if (isUsableAssetBase(fallbackBase)) {
            if (configuredBase != null && isTemporaryTunnelHost(configuredBase.getHost())) {
                log.warn(
                        "Ignoring temporary AI asset base {} and using service.asset.url {}",
                        configuredBase,
                        fallbackBase
                );
            }
            return fallbackBase;
        }

        return null;
    }

    private URI parseAssetBase(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return URI.create(value.trim());
        } catch (IllegalArgumentException exception) {
            log.warn("Invalid asset service base URL '{}': {}", value, exception.getMessage());
            return null;
        }
    }

    private boolean isUsableAssetBase(URI base) {
        return base != null
                && base.getHost() != null
                && base.getScheme() != null
                && !isTemporaryTunnelHost(base.getHost());
    }

    private boolean isTemporaryTunnelHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }

        String normalized = host.trim().toLowerCase(Locale.ROOT);
        return normalized.endsWith(".trycloudflare.com");
    }

    private boolean isLocalOnlyHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }

        String normalized = host.trim().toLowerCase(Locale.ROOT);
        return "localhost".equals(normalized)
                || "asset-service".equals(normalized)
                || "host.docker.internal".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "0.0.0.0".equals(normalized)
                || "::1".equals(normalized)
                || normalized.startsWith("192.168.")
                || normalized.startsWith("10.")
                || normalized.matches("^172\\.(1[6-9]|2\\d|3[0-1])\\..*");
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

