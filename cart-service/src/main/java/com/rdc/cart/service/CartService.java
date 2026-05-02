package com.rdc.cart.service;

import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;

import java.util.List;

public interface CartService {

    CartItemResponse addToCart(Long userId, CartItemRequest request);

    List<CartItemResponse> getCartByUserId(Long userId);

    CartItemResponse updateQuantity(Long userId, Long cartItemId, Integer quantity);

    void removeFromCart(Long userId, Long cartItemId);

    void clearCart(Long userId);

    void removeDesignFromAllCarts(Long designId);

    long getCartItemCount(Long userId);
}
