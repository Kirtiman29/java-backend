package com.rdc.subscription.dto.internal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActivateSubscriptionRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long planId;
}