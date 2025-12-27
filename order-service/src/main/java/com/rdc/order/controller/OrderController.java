package com.rdc.order.controller;

import com.rdc.order.dto.OrderResponse;
import com.rdc.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order Controller
 *
 * SECURITY NOTES:
 * 1. userId comes from X-User-Id header (set by auth gateway)
 * 2. User can only access their own orders
 * 3. Order is created from cart - NO items from frontend
 *
 * API Contract:
 * POST   /api/orders              - Create order from cart
 * GET    /api/orders              - Get user's orders
 * GET    /api/orders/{id}         - Get specific order
 * POST   /api/orders/{id}/cancel  - Cancel order
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create order from user's cart.
     *
     * - No request body needed
     * - Items are fetched from Cart Service
     * - Price is locked at order creation
     * - Cart is cleared after order creation
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") Long userId) {

        OrderResponse response = orderService.createOrder(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all orders for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrdersForUser(
            @RequestHeader("X-User-Id") Long userId) {

        List<OrderResponse> responses = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a specific order by ID.
     * User can only access their own orders.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {

        OrderResponse response = orderService.getOrderById(orderId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an order.
     * Only allowed when order status is CREATED.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader("X-User-Id") Long userId) {

        orderService.cancelOrder(orderId, userId);
        return ResponseEntity.noContent().build();
    }
}
