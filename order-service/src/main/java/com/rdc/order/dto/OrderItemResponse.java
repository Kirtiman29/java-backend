package com.rdc.order.dto;

import lombok.Data;

@Data
public class OrderItemResponse {
    private Long id;
    private Long assetId;
    private String assetUuid;
    private Integer quantity;
    private Long priceCents;    // ✅ Long
}
