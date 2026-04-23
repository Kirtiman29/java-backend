package com.rdc.admin.client;

import com.rdc.admin.dto.ai.SubscriptionAiValidationRequest;
import com.rdc.admin.dto.ai.SubscriptionAiValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class SubscriptionServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.subscription.url}")
    private String subscriptionServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public SubscriptionAiValidationResponse validateAi(Long userId, String toolName, int creditsRequired) {
        String url = subscriptionServiceUrl + "/api/internal/subscriptions/validate-ai";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        SubscriptionAiValidationRequest body = SubscriptionAiValidationRequest.builder()
                .userId(userId)
                .toolName(toolName)
                .creditsRequired(creditsRequired)
                .build();

        HttpEntity<SubscriptionAiValidationRequest> entity = new HttpEntity<>(body, headers);

        ResponseEntity<SubscriptionAiValidationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                SubscriptionAiValidationResponse.class
        );

        return response.getBody();
    }

    public SubscriptionAiValidationResponse consumeAi(Long userId, String toolName, int creditsRequired) {
        String url = subscriptionServiceUrl + "/api/internal/subscriptions/consume-ai";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        SubscriptionAiValidationRequest body = SubscriptionAiValidationRequest.builder()
                .userId(userId)
                .toolName(toolName)
                .creditsRequired(creditsRequired)
                .build();

        HttpEntity<SubscriptionAiValidationRequest> entity = new HttpEntity<>(body, headers);

        ResponseEntity<SubscriptionAiValidationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                SubscriptionAiValidationResponse.class
        );

        return response.getBody();
    }
}
