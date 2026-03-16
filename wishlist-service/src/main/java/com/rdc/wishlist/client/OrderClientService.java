package com.rdc.wishlist.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OrderClientService {
    private final RestTemplate restTemplate;

    @Value("${service.order.url}")
    private String orderServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public boolean hasUserPurchased(Long userId, String assetUuid) {

        try {

            String url = orderServiceUrl +
                    "/api/orders/internal/has-purchased?userId=" +
                    userId + "&assetUuid=" + assetUuid;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Boolean> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, Boolean.class);

            return Boolean.TRUE.equals(response.getBody());

        } catch (Exception e) {

            // fallback: assume NOT purchased
            return false;
        }
    }
}