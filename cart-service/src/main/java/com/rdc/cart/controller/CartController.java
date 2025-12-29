package com.rdc.cart.controller;

import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.security.UserPrincipal;
import com.rdc.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Cart Controller with JWT Authentication
 *
 * Security:
 * - User email is extracted from JWT token (NOT from request body)
 * - All endpoints require valid JWT with USER role
 *
 * Note: Since Auth Service JWT doesn't include userId,
 * we use email hash as a stable user identifier for the cart.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * Add item to cart.
     */
    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addToCart(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request) {

        Long userId = getUserIdFromAuth(authentication);
        CartItemResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all items in user's cart.
     */
    @GetMapping("/items")
    public ResponseEntity<List<CartItemResponse>> getCart(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<CartItemResponse> items = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(items);
    }

    /**
     * Update quantity of a cart item.
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateQuantity(
            Authentication authentication,
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> body) {

        Long userId = getUserIdFromAuth(authentication);
        Integer quantity = body.get("quantity");

        if (quantity == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "quantity is required"));
        }

        if (quantity <= 0) {
            cartService.removeFromCart(userId, itemId);
            return ResponseEntity.noContent().build();
        }

        CartItemResponse response = cartService.updateQuantity(userId, itemId, quantity);
        return ResponseEntity.ok(response);
    }

    /**
     * Remove an item from cart.
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeFromCart(
            Authentication authentication,
            @PathVariable Long itemId) {

        Long userId = getUserIdFromAuth(authentication);
        cartService.removeFromCart(userId, itemId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Clear all items from cart.
     */
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get cart item count.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCartCount(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        long count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Convert email to a stable userId.
     *
     * Since Auth Service JWT doesn't include userId,
     * we generate a stable ID from the email hash.
     *
     * This ensures:
     * - Same email always gets same userId
     * - userId is positive and within Long range
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String email = principal.getEmail();

        // Use Math.abs to ensure positive, and mask to fit in reasonable range
        return Math.abs(email.hashCode()) & 0x7FFFFFFFL;
    }
}