package com.rdc.order.service;

import com.rdc.order.dto.OrderResponse;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(Long userId);
    OrderResponse getOrderById(Long orderId, Long userId);
    OrderResponse getOrderByIdInternal(Long orderId);
    OrderResponse getOrderByIdAdmin(Long orderId);
    List<OrderResponse> getOrdersByUser(Long userId);
    List<OrderResponse> getAllOrders();
    void updateStatus(Long orderId, String status, String transactionId, String paymentMode);
    void cancelOrder(Long orderId, Long userId);
    boolean hasUserPaidForAsset(Long userId, String assetUuid);
    void markDesignAsSoldInternal(Long designId);
    void purgeDesignInternal(Long designId, Long orderId);
}