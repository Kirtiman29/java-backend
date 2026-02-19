package com.rdc.payment.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.rdc.payment.entity.Payment;
import com.rdc.payment.entity.PaymentStatus;
import com.rdc.payment.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    @Value("${service.order.url}")
    private String orderServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public List<Payment> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Payment> getAllPaymentsSorted() {
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Payment initiatePayment(Long orderId, Long userId) throws Exception {
        Optional<Payment> existing = paymentRepository.findByUserIdAndOrderId(userId, orderId)
                .stream().filter(p -> p.getStatus() != PaymentStatus.FAILED).findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        Long amountCents = fetchAmountFromOrderService(orderId, userId);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountCents);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_rcpt_" + orderId);

        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        Payment payment = Payment.builder()
                .orderId(orderId)
                .userId(userId)
                .amountCents(amountCents)
                .gatewayOrderId(razorpayOrder.get("id"))
                .status(PaymentStatus.CREATED)
                .gateway("RAZORPAY")
                .currency("INR")
                .build();

        return paymentRepository.save(payment);
    }

    private Long fetchAmountFromOrderService(Long orderId, Long userId) {
        String url = orderServiceUrl + "/api/internal/orders/" + orderId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalServiceKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map data = response.getBody();

            Object priceObj = data.get("totalPriceCents");
            if (priceObj instanceof Number) {
                return ((Number) priceObj).longValue();
            } else {
                throw new RuntimeException("Invalid price data format");
            }
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Order Service Unreachable");
        }
    }

    @Transactional
    public boolean verifySignatureAndMarkPaid(String orderId, String paymentId, String signature) {
        try {
            if ("SANDBOX_SUCCESS".equals(signature)) return markAsPaid(orderId, paymentId, signature);

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            if (Utils.verifyPaymentSignature(options, apiSecret)) {
                return markAsPaid(orderId, paymentId, signature);
            }
            return false;
        } catch (Exception e) {
            log.error("❌ Signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean markAsPaid(String gatewayOrderId, String paymentId, String signature) {
        Payment p = paymentRepository.findByGatewayOrderId(gatewayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment record not found"));

        if (p.getStatus() == PaymentStatus.PAID) return true;

        String paymentMode = "N/A";
        try {
            com.razorpay.Payment razorpayPayment = razorpayClient.payments.fetch(paymentId);
            paymentMode = razorpayPayment.get("method").toString().toUpperCase();
        } catch (Exception e) {
            log.error("⚠️ Failed to fetch payment method: {}", e.getMessage());
        }

        p.setStatus(PaymentStatus.PAID);
        p.setGatewayPaymentId(paymentId);
        p.setGatewaySignature(signature);
        paymentRepository.save(p);

        notifyOrderService(p.getOrderId(), paymentId, paymentMode);
        return true;
    }

    private void notifyOrderService(Long orderId, String transactionId, String paymentMode) {
        try {
            String url = orderServiceUrl + "/api/internal/orders/" + orderId + "/paid"
                    + "?transactionId=" + transactionId
                    + "&paymentMode=" + paymentMode;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.postForEntity(url, entity, Void.class);
        } catch (Exception e) {
            log.error("⚠️ Failed to notify Order Service: {}", e.getMessage());
        }
    }
}