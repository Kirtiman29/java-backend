package com.rdc.order.service;

import com.rdc.order.dto.CreateOrderRequest;
import com.rdc.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request);

    OrderResponse getOrderById(Long orderId, Long userId);

    List<OrderResponse> getOrdersByUser(Long userId);

    void cancelOrder(Long orderId, Long userId);
}
