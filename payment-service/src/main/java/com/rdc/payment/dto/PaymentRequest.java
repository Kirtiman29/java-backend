package com.rdc.payment.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private String purchaseType;   // ORDER or SUBSCRIPTION
    private Long orderId;          // for ORDER
    private Long planId;           // for SUBSCRIPTION
    private Long userId;           // required for internal/service initiated flows
    private Long amountCents;      // for SUBSCRIPTION
    private String currency;       // default INR
    private String receipt;        // optional custom receipt
}