package com.rdc.admin.dto.ai;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FastApiExecuteResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeSnakeCaseFastApiResponse() throws Exception {
        String json = """
                {
                  "success": true,
                  "request_id": "req-123",
                  "tool_name": "TEXTILE_GENERATOR",
                  "message": "TEXTILE_GENERATOR executed successfully",
                  "output_url": "http://192.168.0.154:8000/output/image_1.png",
                  "output_data": {
                    "generated_images": [
                      {
                        "filename": "image_1.png",
                        "image_url": "http://192.168.0.154:8000/output/image_1.png"
                      },
                      {
                        "filename": "image_2.png",
                        "image_url": "http://192.168.0.154:8000/output/image_2.png"
                      }
                    ]
                  }
                }
                """;

        Map<String, Object> payload = objectMapper.readValue(json, Map.class);
        FastApiExecuteResponse response = FastApiExecuteResponse.fromPayload(payload);

        assertEquals(true, response.getSuccess());
        assertEquals("req-123", response.getRequestId());
        assertEquals("TEXTILE_GENERATOR", response.getToolName());
        assertEquals("http://192.168.0.154:8000/output/image_1.png", response.getOutputUrl());
        assertNotNull(response.getOutputData());
        assertEquals(
                List.of(
                        Map.of(
                                "filename", "image_1.png",
                                "image_url", "http://192.168.0.154:8000/output/image_1.png"
                        ),
                        Map.of(
                                "filename", "image_2.png",
                                "image_url", "http://192.168.0.154:8000/output/image_2.png"
                        )
                ),
                response.getOutputData().get("generated_images")
        );
    }

    @Test
    void shouldTreatStatusSuccessPayloadAsSuccessfulAndExposeRawPayload() throws Exception {
        String json = """
                {
                  "status": "success",
                  "message": "Image upscaled successfully.",
                  "mode": "smart",
                  "output": {
                    "url": "http://127.0.0.1:8000/files/upscale/final.png"
                  }
                }
                """;

        Map<String, Object> payload = objectMapper.readValue(json, Map.class);
        FastApiExecuteResponse response = FastApiExecuteResponse.fromPayload(payload);

        assertEquals(true, response.getSuccess());
        assertEquals("Image upscaled successfully.", response.getMessage());
        assertNotNull(response.getOutputData());
        assertEquals(
                "http://127.0.0.1:8000/files/upscale/final.png",
                ((Map<?, ?>) response.getOutputData().get("output")).get("url")
        );
    }
}
