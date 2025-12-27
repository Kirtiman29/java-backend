package com.rdc.cart.controller;

import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Cart Controller
 *
 * SECURITY NOTES:
 * 1. userId comes from X-User-Id header (set by auth gateway/filter)
 * 2. User can only access their own cart
 * 3. All endpoints are protected (see SecurityConfig)
 *
 * API Contract:
 * POST   /api/cart/items           - Add item to cart
 * GET    /api/cart/items           - Get user's cart
 * PUT    /api/cart/items/{id}      - Update quantity
 * DELETE /api/cart/items/{id}      - Remove item
 * DELETE /api/cart/items           - Clear cart
 * GET    /api/cart/count           - Get item count
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Add item to cart.
     *
     * Request body contains only designId and quantity.
     * Price is fetched from Admin Service internally.
     * userId comes from auth header.
     */
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addToCart(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CartItemRequest request) {

        CartItemResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all items in user's cart.
     */
    @GetMapping("/items")
    public ResponseEntity<List<CartItemResponse>> getCart(
            @RequestHeader("X-User-Id") Long userId) {

        List<CartItemResponse> items = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(items);
    }

    /**
     * Update quantity of a cart item.
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartItemResponse> updateQuantity(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> body) {

        Integer quantity = body.get("quantity");
        if (quantity == null) {
            return ResponseEntity.badRequest().build();
        }

        CartItemResponse response = cartService.updateQuantity(userId, itemId, quantity);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove an item from cart.
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeFromCart(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long itemId) {

        cartService.removeFromCart(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear all items from cart.
     */
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-User-Id") Long userId) {

        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get cart item count.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCartCount(
            @RequestHeader("X-User-Id") Long userId) {

        long count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
