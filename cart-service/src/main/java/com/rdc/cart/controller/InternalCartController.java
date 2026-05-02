package com.rdc.cart.controller;

import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/cart/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalCartController {

    private final CartService cartService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CartItemResponse>> getCartByUserId(
            @PathVariable Long userId,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {
        validateKey(key);
        log.info("Internal request: Fetching cart for userId: {}", userId);
        return ResponseEntity.ok(cartService.getCartByUserId(userId));
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> clearCartByUserId(
            @PathVariable Long userId,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {
        validateKey(key);
        log.info("Internal request: Clearing cart for userId: {}", userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/design/{designId}")
    public ResponseEntity<Void> removeDesignFromAllCarts(
            @PathVariable Long designId,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {
        validateKey(key);
        log.info("Internal request: Removing design {} from all carts", designId);
        cartService.removeDesignFromAllCarts(designId);
        return ResponseEntity.noContent().build();
    }

    private void validateKey(String key) {
        if (internalServiceKey == null || !internalServiceKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service key");
        }
    }
}
