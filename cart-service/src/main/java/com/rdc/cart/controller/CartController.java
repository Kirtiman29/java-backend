package com.rdc.cart.controller;

import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addToCart(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request) {
        Long userId = getUserIdFromAuth(authentication);
        CartItemResponse response = cartService.addToCart(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/items")
    public ResponseEntity<List<CartItemResponse>> getCart(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    // ✅ FIX: Added DELETE mapping for base items path
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateQuantity(
            Authentication authentication,
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> body) {
        Long userId = getUserIdFromAuth(authentication);
        Integer quantity = body.get("quantity");

        if (quantity == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "quantity is required"));
        }

        if (quantity <= 0) {
            cartService.removeFromCart(userId, itemId);
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(cartService.updateQuantity(userId, itemId, quantity));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeFromCart(Authentication authentication, @PathVariable Long itemId) {
        cartService.removeFromCart(getUserIdFromAuth(authentication), itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCartCount(Authentication authentication) {
        long count = cartService.getCartItemCount(getUserIdFromAuth(authentication));
        return ResponseEntity.ok(Map.of("count", count));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        String subject = authentication.getName();
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            log.error("Critical Auth Error: JWT subject is not a numeric ID: {}", subject);
            throw new org.springframework.security.authentication.BadCredentialsException("Invalid User Identity");
        }
    }
}