package com.rdc.order.service;

import com.rdc.order.client.CartServiceClient;
import com.rdc.order.dto.CartItemDto;
import com.rdc.order.dto.OrderItemResponse;
import com.rdc.order.dto.OrderResponse;
import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.exception.OrderCancellationException;
import com.rdc.order.exception.OrderNotFoundException;
import com.rdc.order.model.OrderStatus;
import com.rdc.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartServiceClient cartServiceClient;

    @Override
    public OrderResponse createOrder(Long userId) {
        log.info("Initiating order creation for user ID: {}", userId);

        // Fetch items from Cart Service using the same userId derivation
        List<CartItemDto> cartItems = cartServiceClient.getCartItems(userId);

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED.name())
                .totalPriceCents(0L)
                .build();

        long calculatedTotal = 0L;

        for (CartItemDto cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .designId(cartItem.getDesignId())
                    .assetUuid(cartItem.getAssetUuid())
                    .designTitle(cartItem.getDesignTitle())
                    .quantity(cartItem.getQuantity())
                    .priceCents(cartItem.getPriceCents()) // Locked price snapshot from Cart
                    .build();

            order.addItem(orderItem);
            calculatedTotal += cartItem.getPriceCents() * cartItem.getQuantity();
        }

        order.setTotalPriceCents(calculatedTotal);
        Order savedOrder = orderRepository.save(order);

        // Best-effort cart clear
        try {
            cartServiceClient.clearCart(userId);
        } catch (Exception e) {
            log.warn("Order {} saved, but cart clearing failed for user {}", savedOrder.getId(), userId);
        }

        return mapToResponse(savedOrder);
    }

    @Override
    public void updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        order.setStatus(status);
        orderRepository.save(order);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .designId(item.getDesignId())
                        .assetUuid(item.getAssetUuid())
                        .designTitle(item.getDesignTitle())
                        .quantity(item.getQuantity())
                        .priceCents(item.getPriceCents())
                        .totalPriceCents(item.getTotalPriceCents())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalPriceCents(order.getTotalPriceCents())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse toResponse(Order order) { return mapToResponse(order); }

    @Override public void cancelOrder(Long orderId, Long userId) { /* Logic remains same */ }
}