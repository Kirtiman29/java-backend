package com.rdc.cart.controller;

import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/cart")
@RequiredArgsConstructor
@Slf4j
public class InternalCartController {

    private final CartService cartService;

    /**
     * Get cart items for a user.
     * Used by Order Service to calculate totals and create order lines.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartItemResponse>> getCartByUserId(@PathVariable Long userId) {
        log.info("Internal request: Fetching cart for userId: {}", userId);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    /**
     * Clear cart for a user.
     * Called by Order Service as a best-effort cleanup after order placement. [cite: 237]
     */
    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCartByUserId(@PathVariable Long userId) {
        log.info("Internal request: Clearing cart for userId: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }
}