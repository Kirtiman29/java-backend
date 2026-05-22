package com.rdc.admin.dto.ai;

import lombok.Data;

import java.util.Map;

@Data
public class FastApiExecuteResponse {
    private Boolean success;
    private String requestId;
    private String toolName;
    private String message;
    private String outputUrl;
    private Map<String, Object> outputData;

    @SuppressWarnings("unchecked")
    public static FastApiExecuteResponse fromPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        FastApiExecuteResponse response = new FastApiExecuteResponse();
        response.setSuccess(booleanValue(payload.get("success"), payload.get("status")));
        response.setRequestId(stringValue(payload, "requestId", "request_id"));
        response.setToolName(stringValue(payload, "toolName", "tool_name"));
        response.setMessage(stringValue(payload, "message"));
        response.setOutputUrl(stringValue(payload, "outputUrl", "output_url"));

        Object outputData = payload.containsKey("outputData")
                ? payload.get("outputData")
                : payload.get("output_data");
        if (outputData instanceof Map<?, ?> mapValue) {
            response.setOutputData((Map<String, Object>) mapValue);
        } else {
            response.setOutputData(payload);
        }

        return response;
    }

    private static String stringValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private static Boolean booleanValue(Object... values) {
        for (Object value : values) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof String text) {
                String normalized = text.trim();
                if ("true".equalsIgnoreCase(normalized) || "success".equalsIgnoreCase(normalized)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(normalized) || "failed".equalsIgnoreCase(normalized) || "error".equalsIgnoreCase(normalized)) {
                    return false;
                }
            }
        }
        return null;
    }
}
