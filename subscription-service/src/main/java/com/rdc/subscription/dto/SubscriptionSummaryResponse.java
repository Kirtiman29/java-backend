package com.rdc.subscription.dto;

import com.rdc.subscription.enums.BillingCycle;
import com.rdc.subscription.enums.PlanType;
import com.rdc.subscription.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionSummaryResponse {
    private Long subscriptionId;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Long planId;
    private String planName;
    private PlanType planType;
    private BillingCycle billingCycle;

    private Integer designLimit;
    private Integer creditLimit;
    private Integer availableCredits;
    private Integer usedDesigns;
    private Integer remainingDesigns;
    private java.math.BigDecimal pricePerDesign;
}
