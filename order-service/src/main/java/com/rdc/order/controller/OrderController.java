package com.rdc.order.controller;

import com.rdc.order.dto.OrderResponse;
import com.rdc.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order Controller with Basic Auth
 *
 * userId is derived from authenticated username:
 * - "user" -> userId = 1
 * - "admin" -> userId = 2
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Create order from cart items.
     * Cart items are fetched from Cart Service and cart is cleared after order creation.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        OrderResponse response = orderService.createOrder(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all orders for authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<OrderResponse> orders = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(orders);
    }

    /**
     * Get specific order by ID.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            Authentication authentication,
            @PathVariable Long orderId) {

        Long userId = getUserIdFromAuth(authentication);
        OrderResponse order = orderService.getOrderById(orderId, userId);
        return ResponseEntity.ok(order);
    }

    /**
     * Cancel an order.
     * Only orders with status CREATED can be cancelled.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            Authentication authentication,
            @PathVariable Long orderId) {

        Long userId = getUserIdFromAuth(authentication);
        orderService.cancelOrder(orderId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Convert username to userId
     * In production, this would query the database
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        String username = authentication.getName();
        // Simple mapping for demo - in production, query user DB
        return switch (username) {
            case "user" -> 1L;
            case "admin" -> 2L;
            default -> 1L;
        };
    }
}