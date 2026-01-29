package com.rdc.order.controller;

import com.rdc.order.dto.OrderResponse;
import com.rdc.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        log.info("Creating order for userId: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        log.info("Fetching orders for userId: {}", userId);
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        // Ownership check is handled in the service layer [cite: 153, 154]
        return ResponseEntity.ok(orderService.getOrderById(orderId, userId));
    }

    @GetMapping("/{orderId}/download")
    public ResponseEntity<Map<String, String>> getSecureDownload(@PathVariable Long orderId, @AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        OrderResponse order = orderService.getOrderById(orderId, userId);

        if (!"PAID".equals(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Order must be PAID to download.");
        }

        // Industrial Placeholder for temporary download link [cite: 75]
        return ResponseEntity.ok(Map.of("downloadUrl", "https://assets.rdc.com/temp-access-link"));
    }

    private Long getUserIdFromJwt(Jwt jwt) {
        String subject = jwt.getSubject();
        try {
            // Extracts the numeric ID from the JWT "sub" claim [cite: 76, 77]
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            log.error("Critical Auth Error: Non-numeric sub in JWT: {}", subject);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid User ID in Token");
        }
    }
}