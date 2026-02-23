package com.rdc.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RazorpayConfig {

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String apiSecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            log.error("❌ Razorpay API Key or Secret is missing in environment variables!");
            throw new IllegalStateException("Razorpay credentials must be provided.");
        }

        try {
            RazorpayClient client = new RazorpayClient(apiKey, apiSecret);
            log.info("✅ Razorpay Client initialized successfully with Key ID: {}", apiKey);
            return client;
        } catch (RazorpayException e) {
            log.error("❌ Failed to initialize Razorpay Client: {}", e.getMessage());
            throw e;
        }
    }
}