package com.rdc.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private Long userId;
    private String status;
    private String purchaseType;
    private Instant createdAt;
    private Instant updatedAt;

    // Detailed Totals
    private Long subTotalCents;
    private Long subtotalAmountCents;
    private Long discountAmountCents;
    private Long cgstCents;
    private Long sgstCents;
    private Long igstCents;
    private Long grandTotalCents;
    private Long finalAmountCents;
    private String couponCode;

    // Billing Details
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String billingState;
    private String city;
    private String customerGstin;
    private String invoiceType;

    private String addressOne;
    private String addressTwo;
    private String pincode;
    private String organizationName;

    private List<OrderItemResponse> items;
}
