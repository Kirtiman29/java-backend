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
public class AuthServiceClient {

    private final RestTemplate restTemplate;

    // ✅ FIXED: Removed localhost fallback to ensure it uses the industrial IP from .env
    @Value("${service.auth.url}")
    private String authServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public Map<String, Object> getUserMetadata(Long userId) {
        String url = authServiceUrl + "/api/users/" + userId;
        log.info("📡 Internal Bridge: Fetching user metadata from Auth Service: {}", url);

        try {
            // Prepare Headers with the Bridge Key
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Use exchange to send the headers securely [cite: 1341-1342]
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("❌ Failed to fetch user metadata for ID {}: {}", userId, e.getMessage());
            return null;
        }
    }
}