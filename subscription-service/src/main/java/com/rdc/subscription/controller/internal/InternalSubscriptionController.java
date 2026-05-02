package com.rdc.subscription.controller.internal;

import com.rdc.subscription.dto.internal.*;
import com.rdc.subscription.dto.SubscriptionSummaryResponse;
import com.rdc.subscription.service.EntitlementService;
import com.rdc.subscription.service.SubscriptionQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/subscriptions")
public class InternalSubscriptionController {

    private final EntitlementService entitlementService;
    private final SubscriptionQueryService subscriptionQueryService;

    @PostMapping("/validate-ai")
    public ResponseEntity<AiValidationResponse> validateAi(
            @Valid @RequestBody AiValidationRequest request
    ) {
        return ResponseEntity.ok(entitlementService.validateAiUsage(request));
    }

    @PostMapping("/consume-ai")
    public ResponseEntity<AiValidationResponse> consumeAi(
            @Valid @RequestBody ConsumeAiRequest request
    ) {
        return ResponseEntity.ok(entitlementService.consumeAiUsage(request));
    }

    @PostMapping("/validate-design")
    public ResponseEntity<DesignValidationResponse> validateDesign(
            @Valid @RequestBody DesignValidationRequest request
    ) {
        return ResponseEntity.ok(entitlementService.validateDesignUsage(request));
    }

    @PostMapping("/consume-design")
    public ResponseEntity<DesignValidationResponse> consumeDesign(
            @Valid @RequestBody ConsumeDesignRequest request
    ) {
        return ResponseEntity.ok(entitlementService.consumeDesignUsage(request));
    }

    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<SubscriptionSummaryResponse> getSubscriptionSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionQueryService.getSubscriptionSummary(userId));
    }
}
