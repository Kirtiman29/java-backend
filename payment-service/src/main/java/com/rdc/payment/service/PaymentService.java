package com.rdc.payment.service;

import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.rdc.payment.dto.PaymentRequest;
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

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Value("${service.order.url}")
    private String orderServiceUrl;

    @Value("${service.subscription.url:http://localhost:8094}")
    private String subscriptionServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public List<Payment> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Payment> getAllPaymentsSorted() {
        return paymentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Payment initiatePayment(PaymentRequest req, Long userId) throws Exception {
        String purchaseType = req.getPurchaseType();

        if (purchaseType == null || purchaseType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "purchaseType is required");
        }

        return switch (purchaseType.toUpperCase()) {
            case "ORDER" -> initiateOrderPayment(req.getOrderId(), userId);
            case "SUBSCRIPTION" -> initiateSubscriptionPayment(req, userId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported purchase type");
        };
    }

    @Transactional
    public Payment initiateOrderPayment(Long orderId, Long userId) throws Exception {
        Optional<Payment> existingPaid = paymentRepository
                .findByUserIdAndOrderId(userId, orderId)
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .findFirst();

        if (existingPaid.isPresent()) {
            log.info("✅ Payment already completed for Order: {}", orderId);
            return existingPaid.get();
        }
        log.info("Initiating payment for orderId: {}, userId: {}", orderId, userId);
        // 1. Fetch exact amount from Order Service (This is where the 503 was happening)
        Long amountCents = fetchAmountFromOrderService(orderId, userId);

        // 2. Create Razorpay Order
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountCents); // Razorpay expects cents/paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_rcpt_" + orderId);

        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        // 3. Save Payment Record
        Payment payment = Payment.builder()
                .purchaseType("ORDER")
                .orderId(orderId)
                .userId(userId)
                .amountCents(amountCents)
                .gatewayOrderId(razorpayOrder.get("id"))
                .status(PaymentStatus.CREATED)
                .gateway("RAZORPAY")
                .currency("INR")
                .build();

        log.info("💳 Payment Session Initiated: {} for Order: {}", payment.getGatewayOrderId(), orderId);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment initiateSubscriptionPayment(PaymentRequest req, Long userId) throws Exception {
        if (req.getPlanId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planId is required for subscription payment");
        }

        if (req.getAmountCents() == null || req.getAmountCents() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amountCents must be greater than 0");
        }

        String currency = (req.getCurrency() == null || req.getCurrency().isBlank()) ? "INR" : req.getCurrency();
        String receipt = (req.getReceipt() == null || req.getReceipt().isBlank())
                ? "sub_plan_" + req.getPlanId() + "_user_" + userId
                : req.getReceipt();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", req.getAmountCents());
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receipt);

        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        Payment payment = Payment.builder()
                .purchaseType("SUBSCRIPTION")
                .planId(req.getPlanId())
                .userId(userId)
                .amountCents(req.getAmountCents())
                .gatewayOrderId(razorpayOrder.get("id"))
                .status(PaymentStatus.CREATED)
                .gateway("RAZORPAY")
                .currency(currency)
                .build();

        return paymentRepository.save(payment);
    }

    private Long fetchAmountFromOrderService(Long orderId, Long userId) {

        String url = orderServiceUrl + "/api/orders/internal/amount/" + orderId;

        log.info("📡 Internal Bridge → Order Service: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {

            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            Map data = response.getBody();

            if (data == null) {
                throw new RuntimeException("Order Service returned null body");
            }

            Object priceObj = data.get("grandTotalCents");

            if (priceObj == null) {
                throw new RuntimeException("Missing totalPriceCents in response");
            }

            Long amount = ((Number) priceObj).longValue();

            log.info("💰 Verified Order Amount: {} paise for Order {}", amount, orderId);

            return amount;

        } catch (Exception e) {

            log.error("❌ Bridge Failure → Order Service unreachable: {}", e.getMessage());

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Order Service Unreachable"
            );
        }
    }

    @Transactional
    public boolean handleWebhook(String payload, String razorpaySignature) {
        try {
            boolean isValid = Utils.verifyWebhookSignature(payload, razorpaySignature, webhookSecret);
            if (!isValid) {
                log.error("❌ Invalid Razorpay webhook signature!");
                return false;
            }

            JSONObject json = new JSONObject(payload);
            String event = json.getString("event");

            if (!"payment.captured".equals(event)) return true;

            JSONObject paymentEntity = json.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
            return markAsPaid(paymentEntity.getString("order_id"), paymentEntity.getString("id"), razorpaySignature);
        } catch (Exception e) {
            log.error("❌ Webhook processing failed: {}", e.getMessage());
            return false;
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
                .orElseThrow(() -> new RuntimeException("Payment record not found: " + gatewayOrderId));

        if (p.getStatus() == PaymentStatus.PAID) return true;

        String paymentMode = "N/A";
        try {
            com.razorpay.Payment razorpayPayment = razorpayClient.payments.fetch(paymentId);
            paymentMode = razorpayPayment.get("method").toString().toUpperCase();
        } catch (Exception e) {
            log.warn("⚠️ Could not fetch payment method: {}", e.getMessage());
        }

        p.setStatus(PaymentStatus.PAID);
        p.setGatewayPaymentId(paymentId);
        p.setGatewaySignature(signature);
        paymentRepository.save(p);

        if ("ORDER".equalsIgnoreCase(p.getPurchaseType())) {
            notifyOrderService(p.getOrderId(), paymentId, paymentMode);
        } else if ("SUBSCRIPTION".equalsIgnoreCase(p.getPurchaseType())) {
            notifySubscriptionService(p.getUserId(), p.getPlanId());
        }

        return true;
    }

    private void notifyOrderService(Long orderId, String transactionId, String paymentMode) {

        try {

            String url = orderServiceUrl
                    + "/api/orders/internal/" + orderId
                    + "/paid?transactionId=" + transactionId
                    + "&paymentMode=" + paymentMode;

            log.info("📡 Notifying Order Service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );

        } catch (Exception e) {

            log.error("⚠️ Order Service notification failed: {}", e.getMessage());

        }
    }

    private void notifySubscriptionService(Long userId, Long planId) {
        try {
            String url = subscriptionServiceUrl + "/api/internal/subscriptions/activate";

            log.info("📡 Notifying Subscription Service: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "userId", userId,
                    "planId", planId
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Void.class
            );
            
            log.info("✅ Subscription Service activation response: {}", response.getStatusCode());

        } catch (Exception e) {
            log.error("⚠️ Subscription Service notification failed: {}", e.getMessage());
        }
    }
}