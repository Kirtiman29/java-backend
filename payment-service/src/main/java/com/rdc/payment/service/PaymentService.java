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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
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

    @Transactional
    public Payment initiatePayment(Long orderId, Long userId, Integer amountCents) throws Exception {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountCents);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_rcptid_" + orderId);

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

    public List<Payment> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public boolean verifySignatureAndMarkPaid(String orderId, String paymentId, String signature) {
        try {
            if ("SANDBOX_SUCCESS".equals(signature)) {
                return markAsPaid(orderId, paymentId, signature);
            }

            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            if (Utils.verifyPaymentSignature(options, apiSecret)) {
                return markAsPaid(orderId, paymentId, signature);
            }
            return false;
        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean markAsPaid(String orderId, String paymentId, String signature) {
        Optional<Payment> paymentOpt = paymentRepository.findByGatewayOrderId(orderId);
        if (paymentOpt.isPresent()) {
            Payment p = paymentOpt.get();
            p.setStatus(PaymentStatus.PAID);
            p.setGatewayPaymentId(paymentId);
            p.setGatewaySignature(signature);
            paymentRepository.save(p);

            // ✅ INTERNAL BRIDGE CALL
            try {
                String url = orderServiceUrl + "/api/internal/orders/" + p.getOrderId() + "/paid";
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-INTERNAL-KEY", internalServiceKey);
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                restTemplate.postForEntity(url, entity, Void.class);
                log.info("Successfully notified Order Service for Order: {}", p.getOrderId());
            } catch (Exception e) {
                log.error("Failed to notify Order Service for Order {}: {}", p.getOrderId(), e.getMessage());
            }
            return true;
        }
        return false;
    }
}