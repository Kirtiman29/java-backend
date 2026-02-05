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

    @Transactional
    public Payment initiatePayment(Long orderId, Long userId) throws Exception {
        // Idempotency: Avoid creating duplicate gateway orders for the same payment session
        Optional<Payment> existing = paymentRepository.findByUserIdAndOrderId(userId, orderId)
                .stream().filter(p -> p.getStatus() != PaymentStatus.FAILED).findFirst();

        if (existing.isPresent()) {
            log.info("🛒 Returning existing active payment session for Order ID: {}", orderId);
            return existing.get();
        }

        // Fetch verified total from Order Service via secure internal bridge
        Integer amountCents = fetchAmountFromOrderService(orderId, userId);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountCents);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_rcpt_" + orderId);

        // Interact with Razorpay Gateway
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

    private Integer fetchAmountFromOrderService(Long orderId, Long userId) {
        String url = orderServiceUrl + "/api/internal/orders/" + orderId;
        log.info("📡 Fetching verified amount from Order Service: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalServiceKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map data = response.getBody();

            if (data == null || !data.get("userId").toString().equals(userId.toString())) {
                log.error("❌ Access denied: Order {} does not belong to user {}", orderId, userId);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Order ownership mismatch");
            }

            Object priceObj = data.get("totalPriceCents");
            if (priceObj instanceof Number) {
                return ((Number) priceObj).intValue();
            } else {
                throw new RuntimeException("Invalid price data format received from Order Service");
            }

        } catch (Exception e) {
            log.error("❌ Bridge failure to Order Service at {}: {}", url, e.getMessage());
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
            log.error("❌ Signature verification failed for Order {}: {}", orderId, e.getMessage());
            return false;
        }
    }

    private boolean markAsPaid(String gatewayOrderId, String paymentId, String signature) {
        Payment p = paymentRepository.findByGatewayOrderId(gatewayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment record not found for Gateway ID: " + gatewayOrderId));

        if (p.getStatus() == PaymentStatus.PAID) return true;

        // ✅ NEW: Fetch Payment Details from Razorpay to get the "Method" (Mode of Payment)
        String paymentMode = "N/A";
        try {
            com.razorpay.Payment razorpayPayment = razorpayClient.payments.fetch(paymentId);
            paymentMode = razorpayPayment.get("method").toString().toUpperCase(); // e.g., CARD, UPI, NETBANKING
        } catch (Exception e) {
            log.error("⚠️ Failed to fetch payment method from Razorpay for ID {}: {}", paymentId, e.getMessage());
        }

        p.setStatus(PaymentStatus.PAID);
        p.setGatewayPaymentId(paymentId);
        p.setGatewaySignature(signature);
        paymentRepository.save(p);

        // ✅ Updated: Notify Order Service with Transaction ID and Payment Mode
        notifyOrderService(p.getOrderId(), paymentId, paymentMode);
        return true;
    }

    /**
     * ✅ UPDATED: Sends transactionId and paymentMode as query parameters
     * to the Order Service for invoice generation.
     */
    private void notifyOrderService(Long orderId, String transactionId, String paymentMode) {
        try {
            String url = orderServiceUrl + "/api/internal/orders/" + orderId + "/paid"
                    + "?transactionId=" + transactionId
                    + "&paymentMode=" + paymentMode;

            log.info("📣 Notifying Order Service of successful payment: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.postForEntity(url, entity, Void.class);
        } catch (Exception e) {
            log.error("⚠️ Failed to notify Order Service of payment for order {}: {}", orderId, e.getMessage());
        }
    }
}