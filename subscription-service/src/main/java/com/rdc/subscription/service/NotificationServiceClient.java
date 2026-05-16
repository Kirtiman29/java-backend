package com.rdc.subscription.service;

import com.rdc.subscription.dto.internal.InternalNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.admin.url}")
    private String adminServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public void createUserNotification(InternalNotificationRequest request) {
        String url = adminServiceUrl + "/api/internal/notifications";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-INTERNAL-KEY", internalServiceKey);

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Void.class
            );
        } catch (Exception ex) {
            log.warn("Failed to create notification for user {}: {}", request.getUserId(), ex.getMessage());
        }
    }
}
