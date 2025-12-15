package com.rdc.order.controller;

import com.rdc.order.dto.CreateOrderRequest;
import com.rdc.order.dto.OrderResponse;
import com.rdc.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // =============== CREATE ORDER ===============
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody CreateOrderRequest request,
            @RequestHeader("X-User-Id") Long userId
    ) {
        // userId auth-service se aayega header me
        request.setUserId(userId);
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =============== GET SINGLE ORDER BY ID ===============
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable("orderId") Long orderId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        OrderResponse response = orderService.getOrderById(orderId, userId);
        return ResponseEntity.ok(response);
    }

    // =============== GET ALL ORDERS FOR USER ===============
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrdersForUser(
            @RequestHeader("X-User-Id") Long userId
    ) {
        List<OrderResponse> responses = orderService.getOrdersByUser(userId);
        return ResponseEntity.ok(responses);
    }

    // =============== CANCEL ORDER ===============
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable("orderId") Long orderId,
            @RequestHeader("X-User-Id") Long userId
    ) {
        orderService.cancelOrder(orderId, userId);
        return ResponseEntity.noContent().build();
    }
}
