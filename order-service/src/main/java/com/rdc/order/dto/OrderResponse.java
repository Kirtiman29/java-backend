package com.rdc.order.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class OrderResponse {
    private Long id;
    private Long userId;
    private Long totalPriceCents;
    private String status;
    private Instant createdAt;    // ✅ Instant
    private Instant updatedAt;    // ✅ Instant
    private List<OrderItemResponse> items;
}
