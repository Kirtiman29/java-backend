package com.rdc.admin.service;

import com.rdc.admin.client.FastApiClient;
import com.rdc.admin.client.SubscriptionServiceClient;
import com.rdc.admin.dto.ai.AiToolRequest;
import com.rdc.admin.dto.ai.AiToolResponse;
import com.rdc.admin.dto.ai.FastApiExecuteResponse;
import com.rdc.admin.dto.ai.SubscriptionAiValidationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiOrchestrationServiceTest {

    private final AiToolCostService aiToolCostService = new AiToolCostService();
    private final SubscriptionServiceClient subscriptionServiceClient = mock(SubscriptionServiceClient.class);
    private final FastApiClient fastApiClient = mock(FastApiClient.class);
    private final AiOrchestrationService service =
            new AiOrchestrationService(aiToolCostService, subscriptionServiceClient, fastApiClient);

    @Test
    void shouldRejectGeminiTextToImageWhenUserPromptMissing() {
        AiToolRequest request = new AiToolRequest();
        request.setToolName("GEMINI_TEXT_TO_IMAGE");
        request.setParams(Map.of("style", "floral"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.executeTool(1L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("user_prompt is required for Gemini text to image.", exception.getReason());
    }

    @Test
    void shouldRejectGeminiImageMixWhenInputUrlCountIsInvalid() {
        AiToolRequest request = new AiToolRequest();
        request.setToolName("GEMINI_IMAGE_MIX");
        request.setParams(Map.of(
                "inputUrls", List.of("http://assets/1.png"),
                "prompt", "blend motifs"
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.executeTool(1L, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("Gemini image mix requires 2 or 3 input images.", exception.getReason());
    }

    @Test
    void shouldNormalizeGeminiImageMixOutputUrlAfterSuccessfulExecution() {
        AiToolRequest request = new AiToolRequest();
        request.setToolName("GEMINI_IMAGE_MIX");
        request.setParams(Map.of(
                "inputUrls", List.of(
                        "http://assets/1.png",
                        "http://assets/2.png"
                ),
                "prompt", "blend motifs into a premium textile pattern",
                "num_images", 1,
                "aspect_ratio", "1:1"
        ));

        SubscriptionAiValidationResponse validation = new SubscriptionAiValidationResponse();
        validation.setAllowed(true);

        SubscriptionAiValidationResponse consume = new SubscriptionAiValidationResponse();
        consume.setAllowed(true);
        consume.setAvailableCredits(400);

        FastApiExecuteResponse fastApiResponse = new FastApiExecuteResponse();
        fastApiResponse.setSuccess(true);
        fastApiResponse.setMessage("Mixed images generated successfully.");
        fastApiResponse.setOutputData(Map.of(
                "generated_images", List.of(Map.of(
                        "filename", "mixed_image_abcd.png",
                        "image_url", "/mixed-images/mixed_image_abcd.png"
                ))
        ));

        when(subscriptionServiceClient.validateAi(99L, "GEMINI_IMAGE_MIX", 15)).thenReturn(validation);
        when(fastApiClient.execute(any())).thenReturn(fastApiResponse);
        when(subscriptionServiceClient.consumeAi(99L, "GEMINI_IMAGE_MIX", 15)).thenReturn(consume);
        when(fastApiClient.resolveUrl("/mixed-images/mixed_image_abcd.png"))
                .thenReturn("http://127.0.0.1:8000/mixed-images/mixed_image_abcd.png");

        AiToolResponse response = service.executeTool(99L, request);

        assertTrue(response.isSuccess());
        assertEquals("http://127.0.0.1:8000/mixed-images/mixed_image_abcd.png", response.getOutputUrl());
        assertEquals(400, response.getRemainingCredits());

        var order = inOrder(subscriptionServiceClient, fastApiClient);
        order.verify(subscriptionServiceClient).validateAi(99L, "GEMINI_IMAGE_MIX", 15);
        order.verify(fastApiClient).execute(any());
        order.verify(subscriptionServiceClient).consumeAi(99L, "GEMINI_IMAGE_MIX", 15);
        verify(fastApiClient).resolveUrl("/mixed-images/mixed_image_abcd.png");
    }

    @Test
    void shouldNotConsumeCreditsWhenFastApiFails() {
        AiToolRequest request = new AiToolRequest();
        request.setToolName("GEMINI_TEXT_TO_IMAGE");
        request.setParams(Map.of(
                "user_prompt", "floral jaal textile pattern",
                "num_images", 1
        ));

        SubscriptionAiValidationResponse validation = new SubscriptionAiValidationResponse();
        validation.setAllowed(true);

        FastApiExecuteResponse fastApiResponse = new FastApiExecuteResponse();
        fastApiResponse.setSuccess(false);
        fastApiResponse.setMessage("FastAPI error message");

        when(subscriptionServiceClient.validateAi(eq(7L), eq("GEMINI_TEXT_TO_IMAGE"), eq(10))).thenReturn(validation);
        when(fastApiClient.execute(any())).thenReturn(fastApiResponse);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.executeTool(7L, request)
        );

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatusCode());
        assertEquals("FastAPI error message", exception.getReason());
        verify(subscriptionServiceClient, never()).consumeAi(any(), any(), any());
    }
}
