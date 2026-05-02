package com.rdc.admin.dto.subscription;

import lombok.Data;

@Data
public class SubscriptionDesignValidationResponse {
    private boolean allowed;
    private String message;
    private Integer designLimit;
    private Integer usedDesigns;
    private Integer remainingDesigns;
}
