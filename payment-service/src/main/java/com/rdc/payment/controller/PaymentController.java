package com.rdc.payment.controller;

import com.rdc.payment.dto.PaymentInitResponse;
import com.rdc.payment.dto.PaymentRequest;
import com.rdc.payment.entity.Payment;
import com.rdc.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${razorpay.api.key}")
    private String razorpayKey;

    /**
     * ✅ Get Current User's Payment History
     */
    @GetMapping("/my")
    public ResponseEntity<List<Payment>> getMyPayments(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    /**
     * ✅ Admin Only: Fetch Global Payment Ledger
     */
    @GetMapping("/all")
    public ResponseEntity<List<Payment>> getAllPayments() {
        log.info("Fetching global payment ledger for Admin Dashboard");
        return ResponseEntity.ok(paymentService.getAllPaymentsSorted());
    }

    /**
     * ✅ Initiate Razorpay Order
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentInitResponse> createPayment(
            @RequestBody PaymentRequest req,
            @AuthenticationPrincipal Jwt jwt) throws Exception {

        Long userId = (jwt != null) ? getUserIdFromJwt(jwt) : req.getUserId();

        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required");
        }

        Payment payment = paymentService.initiatePayment(req, userId);

        return ResponseEntity.ok(new PaymentInitResponse(
                payment.getGatewayOrderId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                razorpayKey
        ));
    }

    /**
     * ✅ Frontend-Driven Verification
     * Used for immediate UI feedback after successful checkout.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> response) {
        log.info("Processing manual payment verification for Gateway Order: {}", response.get("razorpay_order_id"));

        boolean isValid = paymentService.verifySignatureAndMarkPaid(
                response.get("razorpay_order_id"),
                response.get("razorpay_payment_id"),
                response.get("razorpay_signature")
        );

        if (isValid) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "FAILED", "message", "Invalid Signature"));
        }
    }

    /**
     * ✅ SECURE WEBHOOK ENDPOINT (New)
     * Handles automated server-side capture events from Razorpay.
     * Ensure this endpoint is permitted in SecurityConfig.java.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> handleRazorpayWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String payload) {

        log.info("Received Razorpay Webhook Event");

        boolean processed = paymentService.handleWebhook(payload, signature);

        return processed
                ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    private Long getUserIdFromJwt(Jwt jwt) {
        String subject = jwt.getSubject();
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            log.error("Critical Auth Error: Non-numeric sub in JWT: {}", subject);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid User ID in Token");
        }
    }
}