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

    @GetMapping("/my")
    public ResponseEntity<List<Payment>> getMyPayments(@AuthenticationPrincipal Jwt jwt) {
        Long userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(paymentService.getPaymentsByUser(userId));
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentInitResponse> createPayment(
            @RequestBody PaymentRequest req,
            @AuthenticationPrincipal Jwt jwt) throws Exception {

        Long userId = getUserIdFromJwt(jwt);
        Payment payment = paymentService.initiatePayment(req.getOrderId(), userId);

        return ResponseEntity.ok(new PaymentInitResponse(
                payment.getGatewayOrderId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                razorpayKey
        ));
    }

    /**
     * ✅ FIX: USER-FACING VERIFICATION ENDPOINT
     * Resolves 404 by providing the expected /api/payments/verify path.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> response) {
        log.info("Processing payment verification for Gateway Order: {}", response.get("razorpay_order_id"));

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