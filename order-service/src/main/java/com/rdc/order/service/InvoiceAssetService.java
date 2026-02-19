package com.rdc.order.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.util.Base64;

@Service
public class InvoiceAssetService {

    public String getLogoBase64() {
        try (InputStream inputStream = new ClassPathResource("static/logo.png").getInputStream()) {

            byte[] bytes = StreamUtils.copyToByteArray(inputStream);

            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            throw new RuntimeException("Invoice logo load failed from classpath:static/logo.png", e);
        }
    }
}