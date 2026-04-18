package com.rdc.subscription.service;

import com.rdc.subscription.dto.InitiateSubscriptionPaymentRequest;
import com.rdc.subscription.dto.InitiateSubscriptionPaymentResponse;
import com.rdc.subscription.dto.payment.PaymentCreateRequest;
import com.rdc.subscription.dto.payment.PaymentCreateResponse;
import com.rdc.subscription.entity.Plan;
import com.rdc.subscription.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionPaymentService {

    private final PlanRepository planRepository;
    private final PaymentServiceClient paymentServiceClient;

    public InitiateSubscriptionPaymentResponse initiatePayment(Long userId, InitiateSubscriptionPaymentRequest request) {
        Plan plan = planRepository.findByIdAndIsActiveTrue(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Active plan not found"));

        PaymentCreateRequest paymentRequest = PaymentCreateRequest.builder()
                .purchaseType("SUBSCRIPTION")
                .planId(plan.getId())
                .amountCents(plan.getPrice().multiply(java.math.BigDecimal.valueOf(100)).longValue())
                .currency("INR")
                .receipt("sub_user_" + userId + "_plan_" + plan.getId())
                .userId(userId)
                .build();

        PaymentCreateResponse paymentResponse = paymentServiceClient.createPaymentOrder(paymentRequest);

        if (paymentResponse == null) {
            throw new RuntimeException("Failed to create payment order");
        }

        return InitiateSubscriptionPaymentResponse.builder()
                .planId(plan.getId())
                .planName(plan.getName())
                .razorpayOrderId(paymentResponse.getGatewayOrderId())
                .amount(paymentResponse.getAmountCents())
                .currency(paymentResponse.getCurrency())
                .key(paymentResponse.getRazorpayKey())
                .message("Payment initiated successfully")
                .build();
    }
}
