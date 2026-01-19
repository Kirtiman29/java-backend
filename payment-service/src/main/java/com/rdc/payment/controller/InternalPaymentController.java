package com.rdc.payment.controller;

import com.rdc.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/payments")
@RequiredArgsConstructor
public class InternalPaymentController {

    private final PaymentService paymentService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPaymentInternal(
            @RequestHeader("X-INTERNAL-KEY") String headerKey,
            @RequestBody Map<String, String> response) {

        if (!internalServiceKey.equals(headerKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid Internal Key");
        }

        boolean isValid = paymentService.verifySignatureAndMarkPaid(
                response.get("razorpay_order_id"),
                response.get("razorpay_payment_id"),
                response.get("razorpay_signature")
        );

        return isValid ? ResponseEntity.ok(Map.of("status", "SUCCESS"))
                : ResponseEntity.status(400).body(Map.of("status", "FAILED"));
    }
}