package com.rdc.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitiateSubscriptionPaymentRequest {

    @NotNull
    private Long planId;
}