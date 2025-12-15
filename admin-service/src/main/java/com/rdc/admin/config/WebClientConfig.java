package com.rdc.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    // 1. Define the WebClient.Builder bean explicitly (The Fix!)
    // Spring Boot doesn't provide this by default in a WebMVC-only application.
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    // 2. Define the specialized WebClient for the Asset Service
    // This method now correctly receives the WebClient.Builder bean we created above.
    @Bean
    public WebClient assetServiceWebClient(WebClient.Builder builder) {
        // You should read this base URL from application.properties/yaml
        String baseUrl = "http://localhost:8081/api/assets"; // Example asset service URL

        return builder
                .baseUrl(baseUrl)
                .build();
    }
}