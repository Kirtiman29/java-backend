package com.rdc.payment.controller;

import com.rdc.payment.dto.PaymentRequest;
import com.rdc.payment.entity.Payment;
import com.rdc.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody PaymentRequest req, @AuthenticationPrincipal Jwt jwt) throws Exception {
        // ✅ DYNAMIC USER ID EXTRACTION
        Long userId = Long.parseLong(jwt.getSubject());
        Payment payment = paymentService.initiatePayment(req.getOrderId(), userId, req.getAmountCents());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> response) {
        boolean isValid = paymentService.verifySignatureAndMarkPaid(
                response.get("razorpay_order_id"),
                response.get("razorpay_payment_id"),
                response.get("razorpay_signature")
        );

        return isValid ? ResponseEntity.ok(Map.of("status", "SUCCESS")) : ResponseEntity.status(400).body(Map.of("status", "FAILED"));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Payment>> getMyPayments(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }
}