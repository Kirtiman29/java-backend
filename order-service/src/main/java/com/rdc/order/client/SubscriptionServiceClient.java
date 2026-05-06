package com.rdc.order.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.subscription.url}")
    private String subscriptionServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public boolean hasActiveSubscription(Long userId) {
        String url = subscriptionServiceUrl + "/api/internal/subscriptions/users/" + userId + "/summary";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                return false;
            }

            Object status = body.get("status");
            return status != null && "ACTIVE".equalsIgnoreCase(String.valueOf(status));
        } catch (Exception ex) {
            log.warn("Failed to fetch subscription summary for user {}: {}", userId, ex.getMessage());
            return false;
        }
    }
}
