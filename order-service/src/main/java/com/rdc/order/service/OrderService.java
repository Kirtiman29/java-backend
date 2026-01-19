package com.rdc.order.service;

import com.rdc.order.dto.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long userId);
    OrderResponse getOrderById(Long orderId, Long userId);
    OrderResponse getOrderByIdInternal(Long orderId);
    List<OrderResponse> getOrdersByUser(Long userId);
    void updateStatus(Long orderId, String status);
    void cancelOrder(Long orderId, Long userId);
    boolean hasUserPaidForAsset(Long userId, String assetUuid);
}