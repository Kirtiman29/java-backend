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
        log.info("Creating order for user {}", userId);
        List<CartItemDto> cartItems = cartServiceClient.getCartItems(userId); // [cite: 416]

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED.name())
                .totalPriceCents(0L)
                .build();

        long totalPriceCents = 0L;
        for (CartItemDto cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .designId(cartItem.getDesignId())
                    .assetUuid(cartItem.getAssetUuid())
                    .designTitle(cartItem.getDesignTitle())
                    .quantity(cartItem.getQuantity())
                    .priceCents(cartItem.getPriceCents())
                    .build();
            order.addItem(orderItem); // [cite: 422]
            totalPriceCents += cartItem.getPriceCents() * cartItem.getQuantity();
        }

        order.setTotalPriceCents(totalPriceCents);
        Order savedOrder = orderRepository.save(order); // [cite: 423]

        try {
            cartServiceClient.clearCart(userId); // [cite: 424]
        } catch (Exception e) {
            log.warn("Failed to clear cart for user {}, order {} created", userId, savedOrder.getId());
        }

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId)); // [cite: 428]
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()); // [cite: 430]
    }

    @Override
    public void cancelOrder(Long orderId, Long userId) {
        log.info("Cancelling order {} for user {}", orderId, userId);
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId)); // [cite: 432]

        if (!OrderStatus.CREATED.name().equals(order.getStatus())) {
            throw new OrderCancellationException("Cannot cancel order with status: " + order.getStatus()); // [cite: 433]
        }

        order.setStatus(OrderStatus.CANCELLED.name());
        orderRepository.save(order);
        log.info("Order {} cancelled successfully", orderId);
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
                .map(this::mapItemToResponse)
                .collect(Collectors.toList()); // [cite: 436]

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

    private OrderItemResponse mapItemToResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .designId(item.getDesignId())
                .assetUuid(item.getAssetUuid())
                .designTitle(item.getDesignTitle())
                .quantity(item.getQuantity())
                .priceCents(item.getPriceCents())
                .totalPriceCents(item.getTotalPriceCents())
                .build(); // [cite: 441]
    }
}