package com.rdc.subscription.dto.payment;

import lombok.Data;

@Data
public class PaymentCreateResponse {
    private String gatewayOrderId;
    private Long amountCents;
    private String currency;
    private String razorpayKey;
}
