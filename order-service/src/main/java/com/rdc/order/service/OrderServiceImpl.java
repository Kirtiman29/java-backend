package com.rdc.order.service;

import com.rdc.order.dto.CreateOrderRequest;
import com.rdc.order.dto.OrderItemRequest;
import com.rdc.order.dto.OrderItemResponse;
import com.rdc.order.dto.OrderResponse;
import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.repository.OrderItemRepository;
import com.rdc.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        // New order
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus("CREATED");
        order.setCreatedAt(Instant.now());   // ✅ Instant
        order.setUpdatedAt(Instant.now());   // ✅ Instant

        // IMPORTANT: init list to avoid NPE
        order.setItems(new ArrayList<>());

        long totalPrice = 0L;

        if (request.getItems() != null) {
            for (OrderItemRequest itemReq : request.getItems()) {
                OrderItem item = new OrderItem();
                item.setOrder(order);
                item.setAssetId(itemReq.getAssetId());
                item.setAssetUuid(itemReq.getAssetUuid());
                item.setQuantity(itemReq.getQuantity());
                item.setPriceCents(itemReq.getPriceCents());  // ✅ Long / long compatible

                order.getItems().add(item);

                // qty * price
                totalPrice += (long) itemReq.getQuantity() * itemReq.getPriceCents();
            }
        }

        order.setTotalPriceCents(totalPrice);

        Order saved = orderRepository.save(order);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found for this user"));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<OrderResponse> responses = new ArrayList<>();
        for (Order order : orders) {
            responses.add(mapToResponse(order));
        }
        return responses;
    }

    @Override
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new RuntimeException("Order not found for this user"));
        order.setStatus("CANCELLED");
        order.setUpdatedAt(Instant.now());   // ✅ Instant
        orderRepository.save(order);
    }

    // ================== MAPPING ==================

    private OrderResponse mapToResponse(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setUserId(order.getUserId());
        resp.setTotalPriceCents(order.getTotalPriceCents());
        resp.setStatus(order.getStatus());
        resp.setCreatedAt(order.getCreatedAt());   // ✅ both Instant
        resp.setUpdatedAt(order.getUpdatedAt());   // ✅ both Instant

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                OrderItemResponse ir = new OrderItemResponse();
                ir.setId(item.getId());
                ir.setAssetId(item.getAssetId());
                ir.setAssetUuid(item.getAssetUuid());
                ir.setQuantity(item.getQuantity());
                ir.setPriceCents(item.getPriceCents());   // ✅ Long/long OK
                itemResponses.add(ir);
            }
        }
        resp.setItems(itemResponses);

        return resp;
    }
}
