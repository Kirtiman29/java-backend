package com.rdc.subscription.dto.internal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DesignValidationRequest {

    @NotNull
    private Long userId;
}