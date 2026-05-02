package com.rdc.admin.client;

import com.rdc.admin.dto.order.SubscriptionDownloadOrderRequest;
import com.rdc.admin.dto.order.SubscriptionDownloadOrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class OrderServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.order.url}")
    private String orderServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public SubscriptionDownloadOrderResponse createSubscriptionDownloadOrder(SubscriptionDownloadOrderRequest request) {
        String url = orderServiceUrl + "/api/internal/orders/subscription-downloads";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        HttpEntity<SubscriptionDownloadOrderRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<SubscriptionDownloadOrderResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                SubscriptionDownloadOrderResponse.class
        );

        return response.getBody();
    }
}
