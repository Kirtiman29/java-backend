package com.rdc.subscription.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InitiateSubscriptionPaymentResponse {
    private Long planId;
    private String planName;
    private String razorpayOrderId;
    private Long amount;
    private String currency;
    private String key;
    private String message;
}
