package com.rdc.admin.service;

import com.rdc.admin.client.FastApiClient;
import com.rdc.admin.client.SubscriptionServiceClient;
import com.rdc.admin.dto.ai.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiOrchestrationService {

    private static final String UPSCALE = "UPSCALE";
    private static final String GEMINI_TEXT_TO_IMAGE = "GEMINI_TEXT_TO_IMAGE";
    private static final String GEMINI_IMAGE_TO_IMAGE = "GEMINI_IMAGE_TO_IMAGE";
    private static final String GEMINI_IMAGE_MIX = "GEMINI_IMAGE_MIX";
    private static final List<String> SUPPORTED_UPSCALE_MODES = List.of("smart", "double", "textile");

    private static final List<String> GEMINI_IMAGE_ASPECT_RATIOS = List.of(
            "1:1", "2:3", "3:2", "3:4", "4:3", "4:5", "5:4", "9:16", "16:9", "21:9"
    );

    private static final List<String> GEMINI_IMAGE_MIX_ASPECT_RATIOS = List.of(
            "1:1", "3:4", "4:3", "9:16", "16:9"
    );

    private final AiToolCostService aiToolCostService;
    private final SubscriptionServiceClient subscriptionServiceClient;
    private final FastApiClient fastApiClient;

    public AiToolResponse executeTool(Long userId, AiToolRequest request) {
        return executeTool(userId, null, request);
    }

    public AiToolResponse executeTool(Long userId, String authToken, AiToolRequest request) {
        validateRequest(request);

        Map<String, Object> params = request.getParams() != null ? request.getParams() : Collections.emptyMap();
        int cost = aiToolCostService.getCost(request.getToolName(), params);

        SubscriptionAiValidationResponse validation =
                subscriptionServiceClient.validateAi(userId, request.getToolName(), cost);

        if (validation == null || !validation.isAllowed()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    validation != null ? validation.getMessage() : "AI validation failed"
            );
        }

        FastApiExecuteRequest fastApiRequest = FastApiExecuteRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userId(userId)
                .toolName(mapToolName(request.getToolName()))
                .inputUrl(request.getInputUrl())
                .params(params)
                .authToken(authToken)
                .build();

        FastApiExecuteResponse fastApiResponse = fastApiClient.execute(fastApiRequest);

        if (fastApiResponse == null || !Boolean.TRUE.equals(fastApiResponse.getSuccess())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    fastApiResponse != null ? fastApiResponse.getMessage() : "FastAPI execution failed"
            );
        }

        SubscriptionAiValidationResponse consume =
                subscriptionServiceClient.consumeAi(userId, request.getToolName(), cost);
        if (consume == null || !consume.isAllowed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, validationMessage(consume));
        }

        String outputUrl = normalizeOutputUrl(fastApiResponse);

        return AiToolResponse.builder()
                .success(true)
                .toolName(request.getToolName())
                .message(fastApiResponse.getMessage())
                .outputUrl(outputUrl)
                .outputData(fastApiResponse.getOutputData())
                .remainingCredits(consume.getAvailableCredits())
                .build();
    }

    private String mapToolName(String toolName) {
        if ("IMAGE_TO_IMAGE".equals(toolName)) {
            return "TEXTILE_GENERATOR";
        }
        return toolName;
    }

    private void validateRequest(AiToolRequest request) {
        if (request == null || isBlank(request.getToolName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "toolName is required.");
        }

        Map<String, Object> params = request.getParams() != null ? request.getParams() : Collections.emptyMap();
        switch (request.getToolName()) {
            case UPSCALE -> validateUpscale(request, params);
            case GEMINI_TEXT_TO_IMAGE -> validateGeminiTextToImage(params);
            case GEMINI_IMAGE_TO_IMAGE -> validateGeminiImageToImage(request, params);
            case GEMINI_IMAGE_MIX -> validateGeminiImageMix(params);
            default -> {
                // No extra gateway validation needed for other tools.
            }
        }
    }

    private void validateUpscale(AiToolRequest request, Map<String, Object> params) {
        if (isBlank(request.getInputUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inputUrl is required for upscale.");
        }

        String mode = getString(params, "mode");
        if (isBlank(mode)) {
            mode = getString(params, "model");
        }
        if (isBlank(mode)) {
            mode = getString(params, "upscaleModel");
        }
        if (isBlank(mode)) {
            mode = getString(params, "upscale_model");
        }
        if (isBlank(mode)) {
            mode = getString(params, "modelType");
        }
        if (isBlank(mode)) {
            mode = getString(params, "model_type");
        }
        if (isBlank(mode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "upscale mode is required. Allowed: " + String.join(", ", SUPPORTED_UPSCALE_MODES)
            );
        }

        String normalized = mode.trim().toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        if ("double".equals(normalized)
                || "double_upscale".equals(normalized)
                || "textile".equals(normalized)
                || "smart".equals(normalized)
                || "smart_upscale".equals(normalized)) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Unsupported upscale mode. Allowed: " + String.join(", ", SUPPORTED_UPSCALE_MODES)
        );
    }

    private void validateGeminiTextToImage(Map<String, Object> params) {
        if (isBlank(getString(params, "user_prompt"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_prompt is required for Gemini text to image.");
        }

        validateAspectRatio(getString(params, "aspect_ratio"), GEMINI_IMAGE_ASPECT_RATIOS,
                "Invalid aspect_ratio for Gemini text to image.");
    }

    private void validateGeminiImageToImage(AiToolRequest request, Map<String, Object> params) {
        if (isBlank(request.getInputUrl())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inputUrl is required for Gemini image to image.");
        }

        validateAspectRatio(getString(params, "aspect_ratio"), GEMINI_IMAGE_ASPECT_RATIOS,
                "Invalid aspect_ratio for Gemini image to image.");
    }

    private void validateGeminiImageMix(Map<String, Object> params) {
        List<String> inputUrls = getStringList(params, "inputUrls");
        if (inputUrls.size() < 2 || inputUrls.size() > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini image mix requires 2 or 3 input images.");
        }

        if (isBlank(getString(params, "prompt"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "prompt is required for Gemini image mix.");
        }

        validateAspectRatio(getString(params, "aspect_ratio"), GEMINI_IMAGE_MIX_ASPECT_RATIOS,
                "Invalid aspect_ratio for Gemini image mix.");
    }

    private void validateAspectRatio(String aspectRatio, List<String> allowed, String message) {
        if (isBlank(aspectRatio)) {
            return;
        }

        if (!allowed.contains(aspectRatio.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String normalizeOutputUrl(FastApiExecuteResponse response) {
        String candidate = firstNonBlank(
                response.getOutputUrl(),
                nestedString(response.getOutputData(), "outputUrl"),
                nestedString(response.getOutputData(), "output_url"),
                nestedString(nestedMap(response.getOutputData(), "output"), "url"),
                nestedString(nestedMap(response.getOutputData(), "artifacts"), "finalUrl"),
                nestedString(nestedMap(nestedMap(response.getOutputData(), "artifacts"), "final"), "url"),
                firstStringFromList(response.getOutputData(), "image_urls"),
                firstStringFromList(response.getOutputData(), "imageUrls"),
                firstGeneratedImageUrl(response.getOutputData(), "generated_images"),
                firstGeneratedImageUrl(response.getOutputData(), "generatedImages")
        );

        return fastApiClient.resolveUrl(candidate);
    }

    private String firstGeneratedImageUrl(Map<String, Object> data, String key) {
        if (data == null) {
            return null;
        }

        Object value = data.get(key);
        if (!(value instanceof List<?> items) || items.isEmpty()) {
            return null;
        }

        Object first = items.getFirst();
        if (!(first instanceof Map<?, ?> firstMap)) {
            return null;
        }

        Object imageUrl = firstMap.get("image_url");
        if (!(imageUrl instanceof String) || ((String) imageUrl).isBlank()) {
            imageUrl = firstMap.get("imageUrl");
        }

        return imageUrl instanceof String text && !text.isBlank() ? text : null;
    }

    private String firstStringFromList(Map<String, Object> data, String key) {
        if (data == null) {
            return null;
        }

        Object value = data.get(key);
        if (!(value instanceof List<?> items) || items.isEmpty()) {
            return null;
        }

        Object first = items.getFirst();
        return first instanceof String text && !text.isBlank() ? text : null;
    }

    private String nestedString(Map<String, Object> data, String key) {
        if (data == null) {
            return null;
        }

        Object value = data.get(key);
        return value instanceof String text && !text.isBlank() ? text : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> data, String key) {
        if (data == null) {
            return null;
        }

        Object value = data.get(key);
        return value instanceof Map<?, ?> mapValue ? (Map<String, Object>) mapValue : null;
    }

    private List<String> getStringList(Map<String, Object> params, String key) {
        Object value = params.get(key);
        if (!(value instanceof List<?> items)) {
            return List.of();
        }

        return items.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .toList();
    }

    private String getString(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value instanceof String text ? text : null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String validationMessage(SubscriptionAiValidationResponse validation) {
        return validation != null && !isBlank(validation.getMessage())
                ? validation.getMessage()
                : "AI credit consumption failed";
    }
}
