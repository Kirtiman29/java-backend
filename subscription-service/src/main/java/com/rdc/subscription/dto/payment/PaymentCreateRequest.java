package com.rdc.subscription.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentCreateRequest {
    private String purchaseType;
    private Long orderId;
    private Long planId;
    private Long userId;
    private Long amountCents;
    private String currency;
    private String receipt;
}