package com.rdc.subscription.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseSubscriptionResponse {
    private Long planId;
    private String planName;
    private String message;
}