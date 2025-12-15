package com.rdc.cart.service;

import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;

import java.util.List;

public interface CartService {

    CartItemResponse addToCart(CartItemRequest request);

    List<CartItemResponse> getCartByUserId(Long userId);

    void removeFromCart(Long cartItemId);
}
