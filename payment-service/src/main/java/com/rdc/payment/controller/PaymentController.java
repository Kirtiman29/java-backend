package com.rdc.payment.controller;

import com.rdc.payment.dto.PaymentRequest;
import com.rdc.payment.entity.Payment;
import com.rdc.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestBody PaymentRequest req) throws Exception {
        // Fixed method call to match: Long, Long, Integer
        Payment payment = paymentService.initiatePayment(req.getOrderId(), 1L, req.getAmountCents());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> response) {
        String razorpayOrderId = response.get("razorpay_order_id");
        String razorpayPaymentId = response.get("razorpay_payment_id");
        String signature = response.get("razorpay_signature");

        boolean isValid = paymentService.verifySignatureAndMarkPaid(razorpayOrderId, razorpayPaymentId, signature);

        if (isValid) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } else {
            return ResponseEntity.status(400).body(Map.of("status", "FAILED"));
        }
    }
}