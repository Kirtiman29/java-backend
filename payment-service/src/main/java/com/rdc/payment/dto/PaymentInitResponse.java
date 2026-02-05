package com.rdc.payment.dto;

public record PaymentInitResponse(
        String gatewayOrderId,
        Long amountCents,
        String currency,
        String razorpayKey
) {}