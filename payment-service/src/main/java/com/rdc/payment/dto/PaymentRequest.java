package com.rdc.payment.dto;

import lombok.Data;

@Data
public class PaymentRequest {
    private Long orderId;
    private Integer amountCents;
}