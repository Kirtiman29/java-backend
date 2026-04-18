package com.rdc.subscription.service;

import com.rdc.subscription.dto.payment.PaymentCreateRequest;
import com.rdc.subscription.dto.payment.PaymentCreateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.payment.url}")
    private String paymentServiceUrl;

    public PaymentCreateResponse createPaymentOrder(PaymentCreateRequest request) {
        String url = paymentServiceUrl + "/api/payments/create";
        log.info("Creating payment order via Payment Service: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaymentCreateRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<PaymentCreateResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                PaymentCreateResponse.class
        );

        return response.getBody();
    }
}