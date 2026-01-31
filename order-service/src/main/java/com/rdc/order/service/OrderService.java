package com.rdc.order.service;

import com.rdc.order.dto.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long userId);
    OrderResponse getOrderById(Long orderId, Long userId);
    OrderResponse getOrderByIdInternal(Long orderId);
    OrderResponse getOrderByIdAdmin(Long orderId); // ✅ Added for Admin Detail
    List<OrderResponse> getOrdersByUser(Long userId);
    List<OrderResponse> getAllOrders(); // ✅ Added for Admin Dashboard
    void updateStatus(Long orderId, String status);
    void cancelOrder(Long orderId, Long userId);
    boolean hasUserPaidForAsset(Long userId, String assetUuid);
}