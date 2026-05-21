package com.rdc.admin.client;

import com.rdc.admin.dto.ai.FastApiExecuteRequest;
import com.rdc.admin.dto.ai.FastApiExecuteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class FastApiClient {

    private final RestTemplate restTemplate;

    @Value("${service.fastapi.url}")
    private String fastApiBaseUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public FastApiExecuteResponse execute(FastApiExecuteRequest request) {
        String url = fastApiBaseUrl.replaceAll("/+$", "") + "/internal/ai/execute";
        log.info("Routing AI tool {} to {}", request.getToolName(), url);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-INTERNAL-KEY", internalServiceKey);

        HttpEntity<FastApiExecuteRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<FastApiExecuteResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                FastApiExecuteResponse.class
        );

        return response.getBody();
    }

    public String resolveUrl(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) {
            return null;
        }

        String trimmed = urlOrPath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        String baseUrl = fastApiBaseUrl.replaceAll("/+$", "");
        return trimmed.startsWith("/") ? baseUrl + trimmed : baseUrl + "/" + trimmed;
    }
}
