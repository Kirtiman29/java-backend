package com.rdc.subscription.controller;

import com.rdc.subscription.dto.SubscriptionSummaryResponse;
import com.rdc.subscription.service.SubscriptionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
public class MySubscriptionController {

    private final SubscriptionQueryService subscriptionQueryService;

    @GetMapping("/me")
    public ResponseEntity<SubscriptionSummaryResponse> getMySubscription(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(subscriptionQueryService.getMySubscription(userId));
    }

    @GetMapping("/me/usage")
    public ResponseEntity<SubscriptionSummaryResponse> getMyUsage(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(subscriptionQueryService.getMySubscription(userId));
    }

    @GetMapping("/me/credits")
    public ResponseEntity<Integer> getMyCredits(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        SubscriptionSummaryResponse response = subscriptionQueryService.getMySubscription(userId);
        return ResponseEntity.ok(response.getAvailableCredits());
    }
}