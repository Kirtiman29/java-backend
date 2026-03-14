package com.rdc.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    // userId is set from X-User-Id header, not from request body
    private Long userId;

    // Note: items are fetched from Cart Service, NOT from this request
}
