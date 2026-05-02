package com.rdc.admin.client;

import com.rdc.admin.dto.subscription.SubscriptionDesignValidationResponse;
import com.rdc.admin.dto.subscription.SubscriptionSummaryResponse;
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

    public SubscriptionDesignValidationResponse validateDesign(Long userId) {
        return exchangeDesign(userId, "/api/internal/subscriptions/validate-design");
    }

    public SubscriptionDesignValidationResponse consumeDesign(Long userId) {
        return exchangeDesign(userId, "/api/internal/subscriptions/consume-design");
    }

    public SubscriptionSummaryResponse getSubscriptionSummary(Long userId) {
        String url = subscriptionServiceUrl + "/api/internal/subscriptions/users/" + userId + "/summary";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SubscriptionSummaryResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SubscriptionSummaryResponse.class
        );

        return response.getBody();
    }

    public boolean hasActiveDesignAccess(Long userId) {
        if (userId == null) {
            return false;
        }

        SubscriptionSummaryResponse summary = getSubscriptionSummary(userId);
        if (summary == null || summary.getSubscriptionId() == null) {
            return false;
        }

        String planType = summary.getPlanType();
        return "DESIGN".equalsIgnoreCase(planType) || "COMBO".equalsIgnoreCase(planType);
    }

    private SubscriptionDesignValidationResponse exchangeDesign(Long userId, String path) {
        String url = subscriptionServiceUrl + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        HttpEntity<java.util.Map<String, Long>> entity = new HttpEntity<>(
                java.util.Map.of("userId", userId),
                headers
        );

        ResponseEntity<SubscriptionDesignValidationResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                SubscriptionDesignValidationResponse.class
        );

        return response.getBody();
    }
}
