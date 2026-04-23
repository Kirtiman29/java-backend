package com.rdc.admin.dto.ai;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionAiValidationRequest {
    private Long userId;
    private String toolName;
    private Integer creditsRequired;
}
