package com.rdc.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create Order Request DTO
 *
 * SECURITY: Order is created ONLY from cart.
 * Frontend does NOT send items or prices.
 *
 * API Contract:
 * POST /api/orders
 * Header: X-User-Id: 123
 * Body: {} (empty - userId comes from header)
 *
 * Flow:
 * 1. Order Service receives request with userId from header
 * 2. Order Service calls Cart Service to get items
 * 3. Order Service creates order with items from cart
 * 4. Order Service calls Cart Service to clear cart
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    // userId is set from X-User-Id header, not from request body
    private Long userId;

    // Note: items are fetched from Cart Service, NOT from this request
}
