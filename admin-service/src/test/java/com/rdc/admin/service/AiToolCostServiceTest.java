package com.rdc.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiToolCostServiceTest {

    private final AiToolCostService service = new AiToolCostService();

    @Test
    void shouldMultiplyGenerateDesignCostByRequestedOutputCount() {
        int cost = service.getCost("GEMINI_TEXT_TO_IMAGE", Map.of("outputCount", 4));

        assertEquals(40, cost);
    }

    @Test
    void shouldUseGeminiImageToImageBaseCostWhenOutputCountMissing() {
        int cost = service.getCost("GEMINI_IMAGE_TO_IMAGE", Map.of());

        assertEquals(12, cost);
    }

    @Test
    void shouldMultiplyGeminiImageMixCostByNumImagesAlias() {
        int cost = service.getCost("GEMINI_IMAGE_MIX", Map.of("num_images", 2));

        assertEquals(30, cost);
    }

    @Test
    void shouldUsePatternGeneratorBaseCostForSingleOutputWhenCountMissing() {
        int cost = service.getCost("PATTERN_GENERATOR", Map.of());

        assertEquals(7, cost);
    }

    @Test
    void shouldRejectUpscaleWhenModeMissing() {
        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> service.getCost("UPSCALE", Map.of())
        );

        assertEquals("upscale mode is required. Allowed: smart, double, textile", exception.getReason());
    }

    @Test
    void shouldChargeTextileUpscaleWhenTextileModelSelected() {
        int cost = service.getCost("UPSCALE", Map.of("model", "textile"));

        assertEquals(10, cost);
    }

    @Test
    void shouldChargeDoubleUpscaleWhenDoubleModelSelected() {
        int cost = service.getCost("UPSCALE", Map.of("modelType", "double_upscale"));

        assertEquals(15, cost);
    }

    @Test
    void shouldChargeSmartUpscaleWhenSmartModelSelected() {
        int cost = service.getCost("UPSCALE", Map.of("mode", "smart upscale"));

        assertEquals(20, cost);
    }

    @Test
    void shouldIgnoreBatchFlagAndChargeBasedOnSelectedModel() {
        int cost = service.getCost("UPSCALE", Map.of("batch", true, "model", "textile"));

        assertEquals(10, cost);
    }

    @Test
    void shouldRejectUnsupportedUpscaleMode() {
        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> service.getCost("UPSCALE", Map.of("mode", "normal"))
        );

        assertEquals("Unsupported upscale mode. Allowed: smart, double, textile", exception.getReason());
    }
}
