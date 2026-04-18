package com.rdc.subscription.dto.internal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiValidationResponse {
    private boolean allowed;
    private String message;
    private Integer availableCredits;
    private Integer creditsRequired;
}