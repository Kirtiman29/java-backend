package com.rdc.admin.dto.subscription;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubscriptionSummaryResponse {
    private Long subscriptionId;
    private String status;
    private Long planId;
    private String planName;
    private String planType;
    private String billingCycle;
    private Integer designLimit;
    private Integer creditLimit;
    private Integer availableCredits;
    private Integer usedDesigns;
    private Integer remainingDesigns;
    private BigDecimal pricePerDesign;
}
