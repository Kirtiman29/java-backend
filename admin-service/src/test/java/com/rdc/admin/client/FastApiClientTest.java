package com.rdc.admin.client;

import com.rdc.admin.dto.ai.FastApiExecuteRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FastApiClientTest {

    @Test
    void shouldRewriteLoopbackAssetUrlBeforeCallingFastApi() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        org.springframework.web.client.RestTemplate restTemplate = mock(org.springframework.web.client.RestTemplate.class);
        FastApiClient client = new FastApiClient(objectMapper, restTemplate);

        ReflectionTestUtils.setField(client, "fastApiBaseUrl", "http://fastapi:8000");
        ReflectionTestUtils.setField(client, "internalServiceKey", "internal-key");
        ReflectionTestUtils.setField(client, "aiAssetBaseUrl", "https://assets.example.com");

        FastApiExecuteRequest request = FastApiExecuteRequest.builder()
                .requestId("req-1")
                .userId(42L)
                .toolName("TEXTILE_GENERATOR")
                .inputUrl("http://localhost:8090/api/assets/download/abc-123")
                .params(Map.of(
                        "inputUrls", List.of(
                                "http://localhost:8090/api/assets/download/source-1",
                                "http://localhost:8090/api/assets/download/source-2"
                        ),
                        "num_images", 1
                ))
                .build();

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(ResponseEntity.ok("""
                {"success":true,"message":"ok","outputUrl":"/files/result.png"}
                """));

        when(objectMapper.readValue(anyString(), eq(Map.class))).thenReturn(Map.of(
                "success", true,
                "message", "ok",
                "outputUrl", "/files/result.png"
        ));

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        client.execute(request);

        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(String.class)
        );

        HttpEntity<?> sentEntity = entityCaptor.getValue();
        assertNotNull(sentEntity);

        Object body = sentEntity.getBody();
        assertNotNull(body);

        FastApiExecuteRequest sentRequest = (FastApiExecuteRequest) body;
        assertEquals(
                "https://assets.example.com/api/assets/download/abc-123",
                sentRequest.getInputUrl()
        );

        assertEquals(
                List.of(
                        "https://assets.example.com/api/assets/download/source-1",
                        "https://assets.example.com/api/assets/download/source-2"
                ),
                sentRequest.getParams().get("inputUrls")
        );
    }
}
