package com.rdc.subscription.dto;

import com.rdc.subscription.enums.BillingCycle;
import com.rdc.subscription.enums.PlanType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlanResponse {
    private Long id;
    private String name;
    private PlanType planType;
    private BillingCycle billingCycle;
    private BigDecimal price;
    private Integer designLimit;
    private Integer creditLimit;
}