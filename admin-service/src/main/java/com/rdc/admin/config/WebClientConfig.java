package com.rdc.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${service.asset.url}")
    private String assetServiceUrl;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient assetServiceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(assetServiceUrl + "/api/assets")
                .build();
    }
}