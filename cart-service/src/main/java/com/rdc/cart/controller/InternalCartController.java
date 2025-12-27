package com.rdc.cart.controller;

import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Internal Controller for Service-to-Service communication.
 *
 * This controller is used by Order Service to:
 * 1. Fetch user's cart items
 * 2. Clear cart after order creation
 *
 * SECURITY NOTE: In production, secure this with service-to-service auth
 * (e.g., API key, mTLS, or service mesh)
 */
@RestController
@RequestMapping("/internal/cart")
@RequiredArgsConstructor
public class InternalCartController {

    private final CartService cartService;

    /**
     * Get cart items for a user.
     * Called by Order Service before creating order.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartItemResponse>> getCartByUserId(@PathVariable Long userId) {
        List<CartItemResponse> items = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(items);
    }

    /**
     * Clear cart for a user.
     * Called by Order Service after order is created.
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCartByUserId(@PathVariable Long userId) {
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}
