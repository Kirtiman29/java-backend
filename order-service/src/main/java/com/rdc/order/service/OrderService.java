package com.rdc.order.service;

import com.rdc.order.dto.OrderResponse;
import java.util.List;

/**
 * Order Service Interface defining all marketplace operations.
 */
public interface OrderService {
    OrderResponse createOrder(Long userId);
    OrderResponse getOrderById(Long orderId, Long userId);
    List<OrderResponse> getOrdersByUser(Long userId);
    void cancelOrder(Long orderId, Long userId);
    void updateStatus(Long orderId, String status); // Mandatory for bridge
}