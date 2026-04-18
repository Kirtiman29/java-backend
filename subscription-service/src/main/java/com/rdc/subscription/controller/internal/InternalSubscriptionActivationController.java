package com.rdc.subscription.controller.internal;

import com.rdc.subscription.dto.PurchaseSubscriptionResponse;
import com.rdc.subscription.dto.PurchaseSubscriptionRequest;
import com.rdc.subscription.dto.internal.ActivateSubscriptionRequest;
import com.rdc.subscription.service.SubscriptionCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/subscriptions")
public class InternalSubscriptionActivationController {

    private final SubscriptionCommandService subscriptionCommandService;

    @PostMapping("/activate")
    public ResponseEntity<PurchaseSubscriptionResponse> activateSubscription(
            @Valid @RequestBody ActivateSubscriptionRequest request
    ) {
        PurchaseSubscriptionRequest purchaseRequest = new PurchaseSubscriptionRequest();
        purchaseRequest.setPlanId(request.getPlanId());

        return ResponseEntity.ok(
                subscriptionCommandService.createSubscriptionForUser(request.getUserId(), purchaseRequest)
        );
    }
}