package com.rdc.admin.client;

import com.rdc.admin.dto.ai.FastApiExecuteRequest;
import com.rdc.admin.dto.ai.FastApiExecuteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class FastApiClient {

    private final RestTemplate restTemplate;

    @Value("${service.fastapi.url}")
    private String fastApiUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    public FastApiExecuteResponse execute(FastApiExecuteRequest request) {
        String url = fastApiUrl + "/internal/ai/execute";

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
}
