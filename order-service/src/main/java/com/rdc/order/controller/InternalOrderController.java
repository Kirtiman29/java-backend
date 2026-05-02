package com.rdc.order.controller;

import com.rdc.order.dto.OrderResponse;
import com.rdc.order.dto.SubscriptionDownloadOrderRequest;
import com.rdc.order.dto.SubscriptionDownloadOrderResponse;
import com.rdc.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
@Slf4j
public class InternalOrderController {

    private final OrderService orderService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderInternal(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {

        validateKey(key);
        return ResponseEntity.ok(orderService.getOrderByIdInternal(orderId));
    }

    @PostMapping("/{orderId}/paid")
    public ResponseEntity<Void> markOrderAsPaid(
            @PathVariable Long orderId,
            @RequestParam String transactionId,
            @RequestParam String paymentMode,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {

        validateKey(key);
        log.info("Internal bridge: Marking order {} as PAID. Txn: {}, Mode: {}", orderId, transactionId, paymentMode);

        orderService.updateStatus(orderId, "PAID", transactionId, paymentMode);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/designs/{designId}/sold")
    public ResponseEntity<Void> markDesignAsSold(
            @PathVariable Long designId,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {

        validateKey(key);
        log.info("Internal bridge: Request to mark design {} as SOLD", designId);

        orderService.markDesignAsSoldInternal(designId);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/verify-download")
    public ResponseEntity<Map<String, Boolean>> verifyDownload(
            @RequestParam Long userId,
            @RequestParam String assetUuid,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {

        validateKey(key);
        log.info("Internal bridge: Verifying download entitlement for userId: {} and asset: {}", userId, assetUuid);

        boolean allowed = orderService.hasUserPaidForAsset(userId, assetUuid);
        return ResponseEntity.ok(Map.of("allowed", allowed));
    }

    @PostMapping("/subscription-downloads")
    public ResponseEntity<SubscriptionDownloadOrderResponse> createSubscriptionDownloadOrder(
            @RequestBody SubscriptionDownloadOrderRequest request,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {

        validateKey(key);
        return ResponseEntity.ok(orderService.createSubscriptionDownloadOrder(request));
    }

    private void validateKey(String key) {
        if (internalServiceKey == null || !internalServiceKey.equals(key)) {
            log.error("Access Denied: Invalid or missing internal service key");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service key");
        }
    }
}
