package com.rdc.order.controller;

import com.rdc.order.dto.OrderResponse;
import com.rdc.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject()); // Dynamic Extraction
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrders(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }
}