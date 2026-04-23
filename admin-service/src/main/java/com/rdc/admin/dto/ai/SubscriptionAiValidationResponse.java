package com.rdc.admin.dto.ai;

import lombok.Data;

@Data
public class SubscriptionAiValidationResponse {
    private boolean allowed;
    private String message;
    private Integer availableCredits;
    private Integer creditsRequired;
}
