package com.rdc.order.controller;

import com.rdc.order.dto.OrderRequest; // ✅ Added Import
import com.rdc.order.dto.OrderResponse;
import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.exception.OrderNotFoundException;
import com.rdc.order.repository.OrderItemRepository;
import com.rdc.order.repository.OrderRepository;
import com.rdc.order.service.InvoiceGeneratorService;
import com.rdc.order.service.OrderService;
import jakarta.validation.Valid; // ✅ Added for Validation
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceGeneratorService invoiceGeneratorService;

    @Value("${service.asset.url}")
    private String assetServiceBaseUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    /**
     * ✅ INTERNAL BRIDGE: Fetch amount for Payment Service
     */
    @GetMapping("/internal/amount/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderAmountInternal(
            @PathVariable Long orderId,
            @RequestHeader("X-INTERNAL-KEY") String providedKey) {

        if (!internalServiceKey.equals(providedKey)) {
            log.error("❌ Unauthorized internal access attempt to Order #{}", orderId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid Internal Key");
        }

        OrderResponse order = orderService.getOrderByIdAdmin(orderId);

        if (order.getGrandTotalCents() == null || order.getGrandTotalCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order amount not available");
        }

        return ResponseEntity.ok(
                Map.of("grandTotalCents", order.getGrandTotalCents())
        );
    }

    /**
     * ✅ CREATE ORDER (Updated for New Flow)
     * Now accepts @RequestBody with customer name and GSTIN
     */
    @PostMapping({"", "/"})
    public ResponseEntity<Map<String, Object>> createOrder(
            @Valid @RequestBody OrderRequest request, // ✅ Changed from @AuthenticationPrincipal only
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = getUserIdFromJwt(jwt);

        // Security check: Ensure the user is creating an order for themselves
        if (!userId.equals(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User ID mismatch");
        }

        log.info("🚀 Initiating order creation for Customer: {} (userId: {})",
                request.getCustomerName(), userId);

        // Pass the full request DTO to the service
        OrderResponse createdOrder = orderService.createOrder(request);

        log.info("✅ Order created successfully. ID: {}", createdOrder.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("data", createdOrder));
    }

    /**
     * ✅ GET ORDERS
     */
    @GetMapping({"", "/"})
    public ResponseEntity<Map<String, Object>> getOrders(@AuthenticationPrincipal Jwt jwt) {
        List<OrderResponse> orders;
        if (isAdmin(jwt)) {
            log.info("Administrative access: Fetching global ledger");
            orders = orderService.getAllOrders();
        } else {
            Long userId = getUserIdFromJwt(jwt);
            log.info("Fetching orders for userId: {}", userId);
            orders = orderService.getOrdersByUser(userId);
        }
        return ResponseEntity.ok(Map.of("data", orders));
    }

    /**
     * ✅ GET SINGLE ORDER
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId, @AuthenticationPrincipal Jwt jwt) {
        if (orderId == null || "null".equals(orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order ID is missing");
        }

        Long id = Long.parseLong(orderId);
        OrderResponse order;

        if (isAdmin(jwt)) {
            log.info("Administrative detail view for orderId: {}", id);
            order = orderService.getOrderByIdAdmin(id);
        } else {
            Long userId = getUserIdFromJwt(jwt);
            order = orderService.getOrderById(id, userId);
        }

        return ResponseEntity.ok(Map.of("data", order));
    }

    /**
     * ✅ DOWNLOAD INVOICE
     */
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        orderService.getOrderById(orderId, userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        byte[] pdfBytes = invoiceGeneratorService.generateInvoicePdf(order, items);

        log.info("✅ Manual invoice download triggered for Order #{}", orderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Invoice-RDC-" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * ✅ SECURE DOWNLOAD
     */
    @GetMapping("/{orderId}/download")
    public ResponseEntity<Map<String, Object>> getSecureDownload(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        OrderResponse order = orderService.getOrderById(orderId, userId);
        if (!"PAID".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Order must be PAID to download assets.");
        }

        String secureUrl = assetServiceBaseUrl + "/api/assets/download/" + orderId;
        log.info("📡 Secure asset download link generated for Order #{} via: {}", orderId, assetServiceBaseUrl);
        return ResponseEntity.ok(Map.of("data", Map.of("downloadUrl", secureUrl)));
    }

    /**
     * ✅ INTERNAL BRIDGE: Mark order as PAID
     */
    @PostMapping("/internal/{orderId}/paid")
    public ResponseEntity<Void> markOrderPaidInternal(
            @PathVariable Long orderId,
            @RequestParam String transactionId,
            @RequestParam String paymentMode,
            @RequestHeader("X-INTERNAL-KEY") String providedKey) {

        if (!internalServiceKey.equals(providedKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid Internal Key");
        }

        log.info("💰 Internal Bridge: Marking Order #{} as PAID (Txn: {})", orderId, transactionId);
        orderService.updateStatus(orderId, "PAID", transactionId, paymentMode);
        return ResponseEntity.ok().build();
    }

    /* =========================================
       PRIVATE HELPERS
    ========================================= */

    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("role");
        return roles != null && roles.contains("ADMIN");
    }

    private Long getUserIdFromJwt(Jwt jwt) {
        String subject = jwt.getSubject();
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            log.error("Critical Identity Error: Non-numeric sub in JWT: {}", subject);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Industrial User ID");
        }
    }

    @GetMapping("/internal/has-purchased")
    public ResponseEntity<Boolean> hasPurchased(
            @RequestParam Long userId,
            @RequestParam String assetUuid,
            @RequestHeader("X-INTERNAL-KEY") String key) {

        if (!internalServiceKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal key");
        }

        boolean purchased = orderService.hasUserPaidForAsset(userId, assetUuid);

        return ResponseEntity.ok(purchased);
    }
}