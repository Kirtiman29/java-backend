package com.rdc.order.service;

import com.rdc.order.dto.OrderResponse;

import java.util.List;

/**
 * Order Service Interface
 *
 * SECURITY NOTES:
 * 1. Order is created ONLY from cart (no items from frontend)
 * 2. Price is locked at order creation from cart snapshot
 * 3. User can only access their own orders
 */
public interface OrderService {

    /**
     * Create order from user's cart.
     *
     * Flow:
     * 1. Fetch cart items from Cart Service
     * 2. Validate cart is not empty
     * 3. Create order with items from cart (price locked)
     * 4. Clear cart
     *
     * @param userId User ID from auth header
     * @return Created order
     */
    OrderResponse createOrder(Long userId);

    /**
     * Get order by ID.
     * User can only access their own orders.
     */
    OrderResponse getOrderById(Long orderId, Long userId);

    /**
     * Get all orders for a user.
     */
    List<OrderResponse> getOrdersByUser(Long userId);

    /**
     * Cancel an order.
     * Only allowed when status is CREATED.
     */
    void cancelOrder(Long orderId, Long userId);
}
