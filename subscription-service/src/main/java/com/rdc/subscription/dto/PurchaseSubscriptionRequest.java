package com.rdc.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseSubscriptionRequest {

    @NotNull
    private Long planId;
}