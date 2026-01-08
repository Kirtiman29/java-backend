package com.rdc.order.controller;

import com.rdc.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {
    private final OrderService orderService;

    @PostMapping("/{orderId}/paid")
    public ResponseEntity<Void> markOrderAsPaid(@PathVariable Long orderId) {
        // Internal status update called by Payment Service
        orderService.updateStatus(orderId, "PAID");
        return ResponseEntity.ok().build();
    }
}