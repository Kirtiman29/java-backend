package com.rdc.order.controller;

import com.rdc.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderService orderService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @PostMapping("/{orderId}/paid")
    public ResponseEntity<Void> markOrderAsPaid(
            @PathVariable Long orderId,
            @RequestHeader("X-INTERNAL-KEY") String key) {

        // Security check for internal service key
        if (internalServiceKey == null || !internalServiceKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal service key");
        }

        orderService.updateStatus(orderId, "PAID");
        return ResponseEntity.ok().build();
    }
}