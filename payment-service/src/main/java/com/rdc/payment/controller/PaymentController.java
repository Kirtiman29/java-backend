package com.rdc.payment.controller;

import com.rdc.payment.dto.PaymentInitResponse;
import com.rdc.payment.dto.PaymentRequest;
import com.rdc.payment.entity.Payment;
import com.rdc.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments") // Base path: http://localhost:8092/api/payments
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${razorpay.api.key}")
    private String razorpayKey;

    /**
     * Fetch payment history for the authenticated user
     * GET http://localhost:8092/api/payments/my [cite: 371]
     */
    @GetMapping("/my")
    public ResponseEntity<List<Payment>> getMyPayments(@AuthenticationPrincipal Jwt jwt) {
        // Extract userId from JWT subject [cite: 369, 371]
        Long userId = Long.parseLong(jwt.getSubject());
        List<Payment> payments = paymentService.getPaymentsByUser(userId);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentInitResponse> createOrder(@RequestBody PaymentRequest req, @AuthenticationPrincipal Jwt jwt) throws Exception {
        Long userId = Long.parseLong(jwt.getSubject());
        Payment payment = paymentService.initiatePayment(req.getOrderId(), userId, req.getAmountCents());

        return ResponseEntity.ok(new PaymentInitResponse(
                payment.getGatewayOrderId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                razorpayKey
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> response) {
        boolean isValid = paymentService.verifySignatureAndMarkPaid(
                response.get("razorpay_order_id"),
                response.get("razorpay_payment_id"),
                response.get("razorpay_signature")
        );
        return isValid ? ResponseEntity.ok(Map.of("status", "SUCCESS"))
                : ResponseEntity.status(400).body(Map.of("status", "FAILED"));
    }
}