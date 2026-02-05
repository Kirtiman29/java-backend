package com.rdc.order.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.util.Base64;

@Service
public class InvoiceAssetService {

    /**
     * ✅ Production-Safe Logo Loader
     * Loads the logo from src/main/resources/static/logo.png and converts to Base64.
     */
    public String getLogoBase64() {
        // Use try-with-resources to ensure the InputStream is closed correctly
        try (InputStream inputStream = new ClassPathResource("static/logo.png").getInputStream()) {

            // StreamUtils is more memory-efficient for Spring-based byte reading
            byte[] bytes = StreamUtils.copyToByteArray(inputStream);

            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            // Provide a clear error message including the path for easier debugging
            throw new RuntimeException("Invoice logo load failed from classpath:static/logo.png", e);
        }
    }
}