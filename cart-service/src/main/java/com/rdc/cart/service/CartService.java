package com.rdc.cart.service;

import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;

import java.util.List;

public interface CartService {

    /**
     * Add item to cart.
     * Fetches price from Admin Service - NEVER from request.
     *
     * @param userId User ID from auth header
     * @param request Contains designId and quantity only
     * @return Created cart item with price snapshot
     */
    CartItemResponse addToCart(Long userId, CartItemRequest request);

    /**
     * Get all active cart items for a user.
     */
    List<CartItemResponse> getCartByUserId(Long userId);

    /**
     * Update quantity of a cart item.
     * User can only update their own items.
     */
    CartItemResponse updateQuantity(Long userId, Long cartItemId, Integer quantity);

    /**
     * Remove (soft delete) an item from cart.
     * User can only remove their own items.
     */
    void removeFromCart(Long userId, Long cartItemId);

    /**
     * Clear all items from user's cart.
     * Called after order is created.
     */
    void clearCart(Long userId);

    /**
     * Get cart item count for a user.
     */
    long getCartItemCount(Long userId);
}
