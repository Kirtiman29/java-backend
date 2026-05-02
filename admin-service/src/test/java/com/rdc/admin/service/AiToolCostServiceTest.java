package com.rdc.admin.service;

import org.junit.jupiter.api.Test;

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
    void shouldUsePatternGeneratorBaseCostForSingleOutputWhenCountMissing() {
        int cost = service.getCost("PATTERN_GENERATOR", Map.of());

        assertEquals(7, cost);
    }

    @Test
    void shouldChargeStandardUpscaleWhenNoModelProvided() {
        int cost = service.getCost("UPSCALE", Map.of());

        assertEquals(5, cost);
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
    void shouldChargeBatchUpscaleWhenBatchFlagIsTrue() {
        int cost = service.getCost("UPSCALE", Map.of("batch", true, "model", "textile"));

        assertEquals(50, cost);
    }

    @Test
    void shouldChargeBatchUpscaleWhenBatchFlagUsesSupportedStringAlias() {
        int cost = service.getCost("UPSCALE", Map.of("is_batch", "yes"));

        assertEquals(50, cost);
    }
}
