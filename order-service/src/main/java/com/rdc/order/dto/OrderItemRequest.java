package com.rdc.order.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long assetId;
    private String assetUuid;
    private Integer quantity;
    private Long priceCents;   // ✅ Long
}
