package com.rdc.order.controller;

import com.rdc.order.dto.OrderResponse;
import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.exception.OrderNotFoundException;
import com.rdc.order.repository.OrderItemRepository; // ✅ Added
import com.rdc.order.repository.OrderRepository;
import com.rdc.order.service.InvoiceGeneratorService;
import com.rdc.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final OrderItemRepository orderItemRepository; // ✅ Added for explicit item fetching
    private final InvoiceGeneratorService invoiceGeneratorService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        log.info("Creating order for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@AuthenticationPrincipal Jwt jwt) {
        if (isAdmin(jwt)) {
            log.info("Administrative access: Fetching global ledger");
            return ResponseEntity.ok(orderService.getAllOrders());
        }

        Long userId = getUserIdFromJwt(jwt);
        log.info("Fetching orders for userId: {}", userId);
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    /**
     * ✅ FIXED: Support for dual-argument PDF Generation
     * Fetches items explicitly to avoid Hibernate session issues and ensure SKU visibility.
     */
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);

        // 1️⃣ Security Check: Ensure user owns this order
        orderService.getOrderById(orderId, userId);

        // 2️⃣ Fetch actual Order entity
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        // 3️⃣ Fetch Items explicitly (MANDATORY for design identification)
        // This list contains the designIdentifier (SKU) used in the Thymeleaf template.
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        // 4️⃣ Generate PDF using the updated 2-argument method
        //
        byte[] pdfBytes = invoiceGeneratorService.generateInvoicePdf(order, items);

        log.info("✅ Manual invoice download triggered for Order #{}", orderId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Invoice-RDC-" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        if (isAdmin(jwt)) {
            log.info("Administrative detail view for orderId: {}", orderId);
            return ResponseEntity.ok(orderService.getOrderByIdAdmin(orderId));
        }

        Long userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(orderService.getOrderById(orderId, userId));
    }

    @GetMapping("/{orderId}/download")
    public ResponseEntity<Map<String, String>> getSecureDownload(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        OrderResponse order = orderService.getOrderById(orderId, userId);

        if (!"PAID".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Order must be PAID to download assets.");
        }

        return ResponseEntity.ok(Map.of("downloadUrl", "http://localhost:8090/api/assets/download/" + orderId));
    }

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
}