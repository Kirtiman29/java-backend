package com.rdc.order.service;

import com.rdc.order.client.AuthServiceClient;
import com.rdc.order.client.CartServiceClient;
import com.rdc.order.dto.CartItemDto;
import com.rdc.order.dto.OrderItemResponse;
import com.rdc.order.dto.OrderRequest;
import com.rdc.order.dto.OrderResponse;
import com.rdc.order.dto.SubscriptionDownloadOrderRequest;
import com.rdc.order.dto.SubscriptionDownloadOrderResponse;
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
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Value("${service.admin.url}")
    private String adminServiceUrl;

    @Value("${service.cart.url}")
    private String cartServiceUrl;

    @Value("${service.wishlist.url}")
    private String wishlistServiceUrl;

    // Define company state for GST logic
    private static final String COMPANY_STATE = "MAHARASHTRA";

    /* ================= WRITE METHODS ================= */

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        Long userId = request.getUserId();

        // 1. Fetch items from cart
        List<CartItemDto> cartItems = cartServiceClient.getCartItems(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new EmptyCartException("Cart is empty.");
        }

        // 2. Initialize Order with Billing Info
        Order order = Order.builder()
                .userId(userId)
                .status(OrderStatus.CREATED.name())
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .customerPhone(request.getCustomerPhone())
                .organizationName(request.getOrganizationName())
                .addressOne(request.getAddressOne())
                .addressTwo(request.getAddressTwo())
                .city(request.getCity())
                .billingState(request.getBillingState())
                .pincode(request.getPincode())
                .customerGstin(request.getCustomerGstin())
                .build();

        // 3. Process Items and calculate Subtotal
        long subtotalCents = 0L;
        for (CartItemDto item : cartItems) {
            order.addItem(OrderItem.builder()
                    .designId(item.getDesignId())
                    .designIdentifier(item.getDesignIdentifier())
                    .priceCents(item.getPriceCents())
                    .quantity(item.getQuantity())
                    .assetUuid(item.getAssetUuid())
                    .designTitle(item.getDesignTitle())
                    .build());
            long roundedPrice = Math.round(item.getPriceCents() / 100.0) * 100;
            subtotalCents += roundedPrice * item.getQuantity();
        }

        // 4. 🔥 CORE GST LOGIC
        order.setSubTotalCents(Math.round(subtotalCents / 1.18));
        long gst = subtotalCents - order.getSubTotalCents();
        order.setCgstCents(gst / 2);
        order.setSgstCents(gst / 2);
        order.setGrandTotalCents(subtotalCents);

        if (order.getGrandTotalCents() == null || order.getGrandTotalCents() <= 0) {
            throw new RuntimeException("❌ Order total calculation failed");
        }

        // ✅ FIX: Set total amount for DB stability
        order.setTotalAmountCents(order.getGrandTotalCents());

        // 5. Determine B2B vs B2C
        String invoiceType = (request.getCustomerGstin() != null && !request.getCustomerGstin().isBlank())
                ? "B2B" : "B2C";
        order.setInvoiceType(invoiceType);

        // 6. Save to database
        Order saved = orderRepository.saveAndFlush(order);

        // 7. 🔥 REMOVED: cartServiceClient.clearCart(userId);
        // Moved to updateStatus to prevent losing cart on payment failure.

        log.info("✅ Order #{} created. Type: {}, Total: ₹{}", saved.getId(), invoiceType, saved.getGrandTotalCents()/100.0);
        return mapToResponse(saved);
    }

    @Override
    public SubscriptionDownloadOrderResponse createSubscriptionDownloadOrder(SubscriptionDownloadOrderRequest request) {
        Map<String, Object> userMeta = authServiceClient.getUserMetadata(request.getUserId());
        String email = userMeta != null ? (String) userMeta.get("email") : null;
        String name = userMeta != null ? (String) userMeta.get("name") : "Industrial User";

        Order order = Order.builder()
                .userId(request.getUserId())
                .status(OrderStatus.PAID.name())
                .purchaseType("SUBSCRIPTION_DOWNLOAD")
                .customerName(name)
                .customerEmail(email)
                .invoiceType("SUBSCRIPTION")
                .transactionId("SUB-" + request.getUserId() + "-" + request.getDesignId() + "-" + System.currentTimeMillis())
                .paymentMode("SUBSCRIPTION")
                .subTotalCents(0L)
                .cgstCents(0L)
                .sgstCents(0L)
                .grandTotalCents(0L)
                .totalAmountCents(0L)
                .build();

        order.addItem(OrderItem.builder()
                .designId(request.getDesignId())
                .designIdentifier(request.getDesignIdentifier())
                .assetUuid(request.getAssetUuid())
                .designTitle(request.getDesignTitle())
                .priceCents(0L)
                .quantity(1)
                .build());

        Order saved = orderRepository.saveAndFlush(order);

        if (email != null && !email.isBlank()) {
            orderEmailService.sendSubscriptionDownloadConfirmation(saved, email, name, request.getRemainingDesigns());
        }

        return SubscriptionDownloadOrderResponse.builder()
                .orderId(saved.getId())
                .status(saved.getStatus())
                .purchaseType(saved.getPurchaseType())
                .build();
    }

    /* ================= FULFILLMENT & STATUS ================= */

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

            // ✅ FIX: Clear cart only when payment is successful
            cartServiceClient.clearCart(order.getUserId());

            fulfillExclusiveDesignPurchase(order);

            try {
                Map<String, Object> userMeta = authServiceClient.getUserMetadata(order.getUserId());
                String email = (order.getCustomerEmail() != null) ? order.getCustomerEmail() : (String) userMeta.get("email");
                String name = (order.getCustomerName() != null) ? order.getCustomerName() : (String) userMeta.get("name");
                orderEmailService.sendOrderConfirmation(updatedOrder, email, name);
            } catch (Exception e) {
                log.error("❌ Fulfillment Email failed: {}", e.getMessage());
            }
        }
    }

    /* ================= INTERNAL BRIDGES ================= */

    @Override
    public void markDesignAsSoldInternal(Long designId) {
        try {
            String url = adminServiceUrl + "/api/internal/designs/" + designId + "/sold";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            restTemplate.postForEntity(url, new HttpEntity<>(headers), Void.class);
        } catch (Exception e) { log.error("❌ Lock failed: {}", e.getMessage()); }
    }

    @Override
    public void purgeDesignInternal(Long designId, Long orderId) {
        try {
            String url = adminServiceUrl + "/api/internal/designs/" + designId + "/purge?orderId=" + orderId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            restTemplate.postForEntity(url, new HttpEntity<>(headers), Void.class);
        } catch (Exception e) { log.error("❌ Purge failed: {}", e.getMessage()); }
    }

    private void fulfillExclusiveDesignPurchase(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }

        order.getItems().stream()
                .map(OrderItem::getDesignId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(designId -> {
                    markDesignAsSoldInternal(designId);
                    purgeDesignInternal(designId, order.getId());
                    removeDesignFromAllCartsInternal(designId);
                    removeDesignFromAllWishlistsInternal(designId);
                });
    }

    private void removeDesignFromAllCartsInternal(Long designId) {
        try {
            String url = cartServiceUrl + "/api/cart/internal/design/" + designId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception e) {
            log.error("âŒ Failed to remove design {} from carts: {}", designId, e.getMessage());
        }
    }

    private void removeDesignFromAllWishlistsInternal(Long designId) {
        try {
            String url = wishlistServiceUrl + "/api/wishlist/internal/design/" + designId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception e) {
            log.error("âŒ Failed to remove design {} from wishlists: {}", designId, e.getMessage());
        }
    }

    /* ================= MAPPING & READS ================= */

    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .purchaseType(order.getPurchaseType())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .subTotalCents(order.getSubTotalCents())
                .cgstCents(order.getCgstCents())
                .sgstCents(order.getSgstCents())
                .igstCents(order.getIgstCents())
                .grandTotalCents(order.getGrandTotalCents())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .billingState(order.getBillingState())
                .customerGstin(order.getCustomerGstin())
                .invoiceType(order.getInvoiceType())
                .items(order.getItems() == null ? List.of() :
                        order.getItems().stream()
                                .map(item -> OrderItemResponse.builder()
                                        .id(item.getId())
                                        .designId(item.getDesignId())
                                        .designIdentifier(item.getDesignIdentifier())
                                        .assetUuid(item.getAssetUuid())
                                        .designTitle(item.getDesignTitle())
                                        .quantity(item.getQuantity())
                                        .priceCents(item.getPriceCents())
                                        .totalPriceCents((long) item.getPriceCents() * item.getQuantity())
                                        .build())
                                .collect(Collectors.toList())
                )
                .build();
    }

    @Override @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(Long orderId) {
        return orderRepository.findById(orderId).map(this::mapToResponse).orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }

    @Override @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::mapToResponse).collect(Collectors.toList());
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

    @Override @Transactional(readOnly = true)
    public boolean hasUserPaidForAsset(Long userId, String assetUuid) {
        List<Order> paidOrders = orderRepository.findByUserIdAndStatus(userId, OrderStatus.PAID.name());
        return paidOrders.stream()
                .filter(order -> !"SUBSCRIPTION_DOWNLOAD".equalsIgnoreCase(order.getPurchaseType()))
                .flatMap(o -> o.getItems().stream())
                .anyMatch(i -> assetUuid.equals(i.getAssetUuid()));
    }
}
