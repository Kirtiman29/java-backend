package com.rdc.subscription.dto.internal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DesignValidationResponse {
    private boolean allowed;
    private String message;
    private Integer designLimit;
    private Integer usedDesigns;
    private Integer remainingDesigns;
}