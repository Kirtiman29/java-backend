package com.rdc.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import com.rdc.payment.entity.Payment;
import com.rdc.payment.entity.PaymentStatus;
import com.rdc.payment.repo.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentRepository paymentRepository;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    @Transactional
    public Payment initiatePayment(Long orderId, Long userId, Integer amountCents) throws Exception {
        // 1. Create Order in Razorpay
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountCents);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_rcptid_" + orderId);

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        // 2. Map fields and Save Payment record
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

    @Transactional
    public boolean verifySignatureAndMarkPaid(String orderId, String paymentId, String signature) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", orderId);
            options.put("razorpay_payment_id", paymentId);
            options.put("razorpay_signature", signature);

            // Manual signature verification for testing without webhooks
            boolean isValid = Utils.verifyPaymentSignature(options, apiSecret);

            if (isValid) {
                Optional<Payment> paymentOpt = paymentRepository.findByGatewayOrderId(orderId);
                if (paymentOpt.isPresent()) {
                    Payment p = paymentOpt.get();
                    p.setStatus(PaymentStatus.PAID);
                    p.setGatewayPaymentId(paymentId);
                    p.setGatewaySignature(signature);
                    paymentRepository.save(p);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}