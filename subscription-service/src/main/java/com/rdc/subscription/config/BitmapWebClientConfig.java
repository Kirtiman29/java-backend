package com.rdc.subscription.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class BitmapWebClientConfig {

    @Value("${service.bitmap.url:http://localhost:8002}")
    private String bitmapUrl;

    @Value("${service.gemini.url:http://localhost:8000}")
    private String geminiUrl;

    @Value("${service.fastapi.response-timeout-seconds:900}")
    private long fastApiResponseTimeoutSeconds;

    @Value("${service.fastapi.max-in-memory-mb:512}")
    private int fastApiMaxInMemoryMb;

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient bitmapWebClient(WebClient.Builder builder) {
        return buildFastApiClient(builder, bitmapUrl);
    }

    @Bean
    public WebClient geminiWebClient(WebClient.Builder builder) {
        return buildFastApiClient(builder, geminiUrl);
    }

    private WebClient buildFastApiClient(WebClient.Builder builder, String baseUrl) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(fastApiResponseTimeoutSeconds));

        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(fastApiMaxInMemoryMb * 1024 * 1024))
                .build();
    }
}


