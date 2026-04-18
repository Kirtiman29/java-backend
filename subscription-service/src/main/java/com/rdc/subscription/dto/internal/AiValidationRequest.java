package com.rdc.subscription.dto.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiValidationRequest {

    @NotNull
    private Long userId;

    @NotBlank
    private String toolName;

    @NotNull
    @Min(1)
    private Integer creditsRequired;
}