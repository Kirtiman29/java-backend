package com.rdc.order.controller;

import com.rdc.order.dto.OrderResponse;
import com.rdc.order.entity.Order;
import com.rdc.order.exception.OrderNotFoundException;
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
    // ✅ ADDED: Required dependencies for PDF generation
    private final OrderRepository orderRepository;
    private final InvoiceGeneratorService invoiceGeneratorService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        log.info("Creating order for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId));
    }

    /**
     * ✅ HYBRID ACCESS:
     * ADMIN: Fetches ALL orders for the transaction dashboard.
     * USER: Fetches only their personal acquisitions.
     */
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
     * ✅ NEW: Triggered from Frontend for Invoice Download
     */
    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);

        // ✅ SECURITY: Verify the order exists and belongs to the user (or admin)
        // This ensures users can't download other people's invoices.
        orderService.getOrderById(orderId, userId);

        // Re-fetch the actual entity to pass to the generator
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        // Generate the PDF bytes using the generator service
        byte[] pdfBytes = invoiceGeneratorService.generateInvoicePdf(order);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Invoice-RDC-" + orderId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * ✅ HYBRID ACCESS:
     * ADMIN: Can view any order details by ID.
     * USER: Restricted to their own order via Service Layer check.
     */
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

        // Bridge to Asset Service for the temporary industrial design link
        return ResponseEntity.ok(Map.of("downloadUrl", "http://localhost:8090/api/assets/download/" + orderId));
    }

    // ✅ HELPER: Check for ADMIN role in the JWT
    private boolean isAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("role");
        return roles != null && roles.contains("ADMIN");
    }

    private Long getUserIdFromJwt(Jwt jwt) {
        String subject = jwt.getSubject();
        try {
            // Numeric subject mapping from Auth Service
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            log.error("Critical Identity Error: Non-numeric sub in JWT: {}", subject);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Industrial User ID");
        }
    }
}