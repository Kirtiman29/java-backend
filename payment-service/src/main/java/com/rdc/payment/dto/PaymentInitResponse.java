package com.rdc.payment.dto;

public record PaymentInitResponse(
        String gatewayOrderId,
        Integer amountCents,
        String currency,
        String razorpayKey
) {}