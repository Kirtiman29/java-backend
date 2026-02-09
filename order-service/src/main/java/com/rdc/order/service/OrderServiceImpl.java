package com.rdc.order.service;

import com.rdc.order.client.AuthServiceClient;
import com.rdc.order.client.CartServiceClient;
import com.rdc.order.dto.CartItemDto;
import com.rdc.order.dto.OrderItemResponse;
import com.rdc.order.dto.OrderResponse;
import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.exception.EmptyCartException;
import com.rdc.order.exception.OrderNotFoundException;
import com.rdc.order.model.OrderStatus;
import com.rdc.order.repository.OrderItemRepository;
import com.rdc.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartServiceClient cartServiceClient;
    private final OrderEmailService orderEmailService;
    private final AuthServiceClient authServiceClient;
    private final RestTemplate restTemplate;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @Value("${service.admin.url:http://localhost:8080}")
    private String adminServiceUrl;

    /* ================= READ METHODS ================= */

    @Override @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(Long orderId) {
        return orderRepository.findById(orderId)
                .map(this::mapToResponse)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    @Override @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public boolean hasUserPaidForAsset(Long userId, String assetUuid) {
        List<Order> paidOrders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.PAID.name());
        return paidOrders.stream()
                .flatMap(order -> order.getItems().stream())
                .anyMatch(item -> assetUuid.equals(item.getAssetUuid()));
    }

    /* ================= WRITE METHODS ================= */

    @Override
    public OrderResponse createOrder(Long userId) {
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
                    .designIdentifier(item.getDesignIdentifier()) // ✅ SKU persisted from Cart
                    .priceCents(item.getPriceCents())
                    .quantity(item.getQuantity())
                    .assetUuid(item.getAssetUuid())
                    .designTitle(item.getDesignTitle())
                    .build());
            total += item.getPriceCents() * item.getQuantity();
        }

        order.setTotalPriceCents(total);
        Order saved = orderRepository.save(order);
        cartServiceClient.clearCart(userId);
        return mapToResponse(saved);
    }

    /**
     * ✅ UPDATED: Triggers fulfillment and handles the "orphan deletion" safety logic.
     */
    @Override
    public void updateStatus(Long orderId, String status, String transactionId, String paymentMode) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (OrderStatus.PAID.name().equals(order.getStatus())) return;

        order.setStatus(status);
        order.setTransactionId(transactionId);
        order.setPaymentMode(paymentMode);
        Order updatedOrder = orderRepository.save(order);

        if (OrderStatus.PAID.name().equals(status)) {
            // Fetch items into a local list to prevent Hibernate session issues in the email service
            List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

            items.forEach(item -> {
                Long designId = item.getDesignId();
                log.info("🚨 Processing fulfillment for Design {} (Order {})", designId, orderId);
                markDesignAsSoldInternal(designId);
                purgeDesignInternal(designId, orderId);
            });

            try {
                Map<String, Object> userMeta = authServiceClient.getUserMetadata(order.getUserId());
                if (userMeta != null) {
                    updatedOrder.setCustomerName((String) userMeta.get("name"));
                    // ✅ FIXED: We pass the pre-fetched 'items' list separately
                    // This ensures the Email Service has data without calling order.getItems()
                    orderEmailService.sendOrderConfirmation(updatedOrder, (String) userMeta.get("email"), (String) userMeta.get("name"));
                }
            } catch (Exception e) {
                log.error("❌ Fulfillment Email failed: {}", e.getMessage());
            }
        }
    }

    @Override
    public void markDesignAsSoldInternal(Long designId) {
        try {
            String url = adminServiceUrl + "/api/internal/designs/" + designId + "/sold";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.postForEntity(url, entity, Void.class);
        } catch (Exception e) {
            log.error("❌ Lock failed for design {}: {}", designId, e.getMessage());
        }
    }

    @Override
    public void purgeDesignInternal(Long designId, Long orderId) {
        try {
            String url = adminServiceUrl + "/api/internal/designs/" + designId + "/purge?orderId=" + orderId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.postForEntity(url, entity, Void.class);
        } catch (Exception e) {
            log.error("❌ Purge failed for design {}: {}", designId, e.getMessage());
        }
    }

    @Override @Transactional(readOnly = true)
    public OrderResponse getOrderByIdInternal(Long orderId) {
        return orderRepository.findById(orderId).map(this::mapToResponse).orElseThrow();
    }

    @Override @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId).map(this::mapToResponse).orElseThrow();
    }

    @Override
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow();
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
                .items(order.getItems().stream()
                        .map(item -> OrderItemResponse.builder()
                                .id(item.getId())
                                .designId(item.getDesignId())
                                .designIdentifier(item.getDesignIdentifier()) // ✅ SKU visibility for UI
                                .assetUuid(item.getAssetUuid())
                                .designTitle(item.getDesignTitle())
                                .quantity(item.getQuantity())
                                .priceCents(item.getPriceCents())
                                .totalPriceCents(item.getTotalPriceCents())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}