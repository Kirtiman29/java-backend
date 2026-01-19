package com.rdc.order.service;

import com.rdc.order.client.CartServiceClient;
import com.rdc.order.dto.CartItemDto;
import com.rdc.order.dto.OrderResponse;
import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.exception.EmptyCartException;
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
        // 1. Fetch items from Cart Service [cite: 46-48]
        List<CartItemDto> cartItems = cartServiceClient.getCartItems(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty.");
        }

        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED.name())
                .totalPriceCents(0L)
                .build();

        long total = 0L;
        for (CartItemDto item : cartItems) {
            order.addItem(OrderItem.builder()
                    .designId(item.getDesignId())
                    .priceCents(item.getPriceCents())
                    .quantity(item.getQuantity())
                    .assetUuid(item.getAssetUuid())
                    .designTitle(item.getDesignTitle())
                    .build());
            total += item.getPriceCents() * item.getQuantity();
        }

        order.setTotalPriceCents(total);
        Order saved = orderRepository.save(order);

        // 2. Clear cart after successful order creation [cite: 49-51]
        cartServiceClient.clearCart(userId);
        return mapToResponse(saved);
    }

    /**
     * SECURE ENTITLEMENT CHECK
     * Required by Asset Service to verify payment before streaming MASTER_TIFF files.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasUserPaidForAsset(Long userId, String assetUuid) {
        log.info("Verifying purchase for userId: {} and asset: {}", userId, assetUuid);

        // Fetch PAID orders for this numeric userId
        List<Order> paidOrders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.PAID.name());

        return paidOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> assetUuid.equals(item.getAssetUuid()));
    }

    @Override
    public void updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (OrderStatus.PAID.name().equals(order.getStatus())) {
            log.warn("Order {} is already PAID. Skipping update.", orderId);
            return;
        }

        order.setStatus(status);
        orderRepository.save(order);
    }

    @Override @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public OrderResponse getOrderByIdInternal(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    @Override @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    @Override
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!OrderStatus.CREATED.name().equals(order.getStatus())) {
            throw new IllegalStateException("Only CREATED orders can be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED.name());
        orderRepository.save(order);
    }

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalPriceCents(order.getTotalPriceCents())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}