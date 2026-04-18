package com.rdc.subscription.controller;

import com.rdc.subscription.dto.PurchaseSubscriptionRequest;
import com.rdc.subscription.dto.PurchaseSubscriptionResponse;
import com.rdc.subscription.service.SubscriptionCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionCommandService subscriptionCommandService;

    @PostMapping("/purchase")
    public ResponseEntity<PurchaseSubscriptionResponse> purchaseSubscription(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PurchaseSubscriptionRequest request
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(
                subscriptionCommandService.createSubscriptionForUser(userId, request)
        );
    }
}