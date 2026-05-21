package com.rdc.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class AiToolCostService {

    private static final String UPSCALE = "UPSCALE";
    private static final int STANDARD_UPSCALE_COST = 5;
    private static final int TEXTILE_UPSCALE_COST = 10;
    private static final int DOUBLE_UPSCALE_COST = 15;
    private static final int SMART_UPSCALE_COST = 20;
    private static final int BATCH_UPSCALE_COST = 50;

    private static final Set<String> COUNT_BASED_TOOLS = Set.of(
            "TEXTILE_GENERATOR",
            "GEMINI_TEXT_TO_IMAGE",
            "GEMINI_IMAGE_TO_IMAGE",
            "GEMINI_IMAGE_MIX",
            "IMAGE_TO_IMAGE",
            "SDXL_IMAGE_TO_IMAGE",
            "PATTERN_GENERATOR",
            "IMAGE_MIX"
    );

    private static final String[] OUTPUT_COUNT_KEYS = {
            "outputCount",
            "output_count",
            "numOutputs",
            "num_outputs",
            "count",
            "outputs",
            "outputImages",
            "output_images",
            "imageCount",
            "image_count",
            "numImages",
            "num_images",
            "numberOfImages",
            "number_of_images"
    };

    private static final String[] UPSCALE_MODEL_KEYS = {
            "model",
            "upscaleModel",
            "upscale_model",
            "modelType",
            "model_type",
            "type",
            "mode"
    };

    private static final Map<String, Integer> TOOL_COSTS = Map.ofEntries(
            Map.entry("UPSCALE", 5),
            Map.entry("TEXTILE_GENERATOR", 10),
            Map.entry("GEMINI_TEXT_TO_IMAGE", 10),
            Map.entry("GEMINI_IMAGE_TO_IMAGE", 12),
            Map.entry("GEMINI_IMAGE_MIX", 15),
            Map.entry("IMAGE_TO_IMAGE", 10),
            Map.entry("SDXL_IMAGE_TO_IMAGE", 10),
            Map.entry("PATTERN_GENERATOR", 7),
            Map.entry("COLOR_SEPARATION", 4),
            Map.entry("COLORWAY", 4),
            Map.entry("IMAGE_MIX", 8),
            Map.entry("PROMPT_ENHANCER", 2)
    );

    public int getCost(String toolName, Map<String, Object> params) {
        Integer cost = TOOL_COSTS.get(toolName);
        if (cost == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported AI tool: " + toolName);
        }

        if (UPSCALE.equals(toolName)) {
            return resolveUpscaleCost(params);
        }

        if (COUNT_BASED_TOOLS.contains(toolName)) {
            return cost * resolveOutputCount(params);
        }

        return cost;
    }

    private int resolveUpscaleCost(Map<String, Object> params) {
        String model = getStringParam(params, UPSCALE_MODEL_KEYS);

        boolean batch = getBooleanParam(
                params,
                "batch",
                "isBatch",
                "is_batch",
                "batchUpscale",
                "batch_upscale"
        );

        if (batch) {
            return BATCH_UPSCALE_COST;
        }

        if (model == null || model.isBlank()) {
            return STANDARD_UPSCALE_COST;
        }

        String normalized = model.trim().toLowerCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");

        if (normalized.contains("smart")) {
            return SMART_UPSCALE_COST;
        }

        if (normalized.contains("double")) {
            return DOUBLE_UPSCALE_COST;
        }

        if (normalized.contains("textile")) {
            return TEXTILE_UPSCALE_COST;
        }

        return STANDARD_UPSCALE_COST;
    }

    private int resolveOutputCount(Map<String, Object> params) {
        Integer count = getIntegerParam(params, OUTPUT_COUNT_KEYS);
        return count == null || count < 1 ? 1 : count;
    }

    private boolean getBooleanParam(Map<String, Object> params, String... keys) {
        if (params == null || params.isEmpty()) {
            return false;
        }

        for (String key : keys) {
            Object value = params.get(key);

            if (value instanceof Boolean bool) {
                return bool;
            }

            if (value instanceof String text) {
                String normalized = text.trim().toLowerCase(Locale.ROOT);
                if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized)) {
                    return true;
                }
            }

            if (value instanceof Number number) {
                return number.intValue() == 1;
            }
        }

        return false;
    }

    private Integer getIntegerParam(Map<String, Object> params, String... keys) {
        if (params == null || params.isEmpty()) {
            return null;
        }

        for (String key : keys) {
            Object value = params.get(key);
            if (value instanceof Number number) {
                return number.intValue();
            }
            if (value instanceof String text) {
                try {
                    return Integer.parseInt(text.trim());
                } catch (NumberFormatException ignored) {
                    // Ignore invalid values and continue checking aliases.
                }
            }
        }

        return null;
    }

    private String getStringParam(Map<String, Object> params, String... keys) {
        if (params == null || params.isEmpty()) {
            return null;
        }

        for (String key : keys) {
            Object value = params.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }

        return null;
    }
}
