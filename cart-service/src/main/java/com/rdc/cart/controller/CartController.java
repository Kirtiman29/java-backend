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

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addToCart(@Valid @RequestBody CartItemRequest request) {
        CartItemResponse response = cartService.addToCart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/items/user/{userId}")
    public ResponseEntity<List<CartItemResponse>> getCartByUser(@PathVariable Long userId) {
        List<CartItemResponse> items = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(items);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long itemId) {
        cartService.removeFromCart(itemId);
        return ResponseEntity.noContent().build();
    }
}
