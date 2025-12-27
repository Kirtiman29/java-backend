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

/**
 * Order Service Implementation
 *
 * SECURITY NOTES:
 * 1. Order is created ONLY from cart - items are fetched from Cart Service
 * 2. Price is LOCKED at order creation - won't change if design price changes
 * 3. User can only access their own orders
 * 4. userId comes from auth header, not request body
 */
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

        // 1. Fetch cart items from Cart Service
        // This throws EmptyCartException if cart is empty
        List<CartItemDto> cartItems = cartServiceClient.getCartItems(userId);

        log.info("Found {} items in cart for user {}", cartItems.size(), userId);

        // 2. Create order entity
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED.name())
                .totalPriceCents(0L)
                .build();

        // 3. Create order items from cart items
        // Price is LOCKED here - it won't change even if design price changes later
        long totalPriceCents = 0L;

        for (CartItemDto cartItem : cartItems) {
            OrderItem orderItem = OrderItem.builder()
                    .designId(cartItem.getDesignId())
                    .assetUuid(cartItem.getAssetUuid())
                    .designTitle(cartItem.getDesignTitle())
                    .quantity(cartItem.getQuantity())
                    .priceCents(cartItem.getPriceCents())  // Price from cart (already fetched from Admin)
                    .build();

            order.addItem(orderItem);
            totalPriceCents += cartItem.getPriceCents() * cartItem.getQuantity();
        }

        order.setTotalPriceCents(totalPriceCents);

        // 4. Save order
        Order savedOrder = orderRepository.save(order);
        log.info("Created order {} with total {} cents", savedOrder.getId(), totalPriceCents);

        // 5. Clear cart (async-safe - we don't fail if this fails)
        try {
            cartServiceClient.clearCart(userId);
        } catch (Exception e) {
            log.warn("Failed to clear cart for user {}, but order {} was created successfully",
                    userId, savedOrder.getId());
        }

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        log.debug("Fetching order {} for user {}", orderId, userId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found: " + orderId));

        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        log.debug("Fetching all orders for user {}", userId);

        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void cancelOrder(Long orderId, Long userId) {
        log.info("Cancelling order {} for user {}", orderId, userId);

        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found: " + orderId));

        // Only CREATED orders can be cancelled
        if (!OrderStatus.CREATED.name().equals(order.getStatus())) {
            throw new OrderCancellationException(
                    "Cannot cancel order with status: " + order.getStatus() +
                            ". Only orders with status CREATED can be cancelled.");
        }

        order.setStatus(OrderStatus.CANCELLED.name());
        orderRepository.save(order);

        log.info("Order {} cancelled successfully", orderId);
    }

    /**
     * Map Order entity to OrderResponse DTO.
     */
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapItemToResponse)
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

    /**
     * Map OrderItem entity to OrderItemResponse DTO.
     */
    private OrderItemResponse mapItemToResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .designId(item.getDesignId())
                .assetUuid(item.getAssetUuid())
                .designTitle(item.getDesignTitle())
                .quantity(item.getQuantity())
                .priceCents(item.getPriceCents())
                .totalPriceCents(item.getTotalPriceCents())
                .build();
    }
}
