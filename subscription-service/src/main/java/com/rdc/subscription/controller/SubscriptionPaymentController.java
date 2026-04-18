package com.rdc.subscription.controller;

import com.rdc.subscription.dto.InitiateSubscriptionPaymentRequest;
import com.rdc.subscription.dto.InitiateSubscriptionPaymentResponse;
import com.rdc.subscription.service.SubscriptionPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionPaymentController {

    private final SubscriptionPaymentService subscriptionPaymentService;

    @PostMapping("/initiate-payment")
    public ResponseEntity<InitiateSubscriptionPaymentResponse> initiatePayment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody InitiateSubscriptionPaymentRequest request
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(subscriptionPaymentService.initiatePayment(userId, request));
    }
}