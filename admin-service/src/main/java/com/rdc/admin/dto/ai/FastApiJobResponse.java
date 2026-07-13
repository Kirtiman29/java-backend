package com.rdc.admin.dto.ai;

import lombok.Data;

import java.util.Map;

@Data
public class FastApiJobResponse {
    private Boolean success;
    private Long jobId;
    private Long userId;
    private String featureName;
    private String workerType;
    private String status;
    private String outputKey;
    private String errorMessage;
    private String message;
    private Map<String, Object> input;

    @SuppressWarnings("unchecked")
    public static FastApiJobResponse fromPayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        FastApiJobResponse response = new FastApiJobResponse();
        response.setSuccess(booleanValue(payload.get("success"), payload.get("status")));
        response.setJobId(longValue(payload.get("jobId"), payload.get("job_id"), payload.get("id")));
        response.setUserId(longValue(payload.get("userId"), payload.get("user_id")));
        response.setFeatureName(stringValue(payload, "featureName", "feature_name", "toolName", "tool_name"));
        response.setWorkerType(stringValue(payload, "workerType", "worker_type"));
        response.setStatus(stringValue(payload, "status"));
        response.setOutputKey(stringValue(payload, "outputKey", "output_key", "outputUrl", "output_url"));
        response.setErrorMessage(stringValue(payload, "errorMessage", "error_message", "error"));
        response.setMessage(stringValue(payload, "message"));

        Object input = payload.get("input");
        if (input instanceof Map<?, ?> mapValue) {
            response.setInput((Map<String, Object>) mapValue);
        }

        return response;
    }

    public static FastApiJobResponse failure(String message) {
        FastApiJobResponse response = new FastApiJobResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrorMessage(message);
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

    private static Long longValue(Object... values) {
        for (Object value : values) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Long.parseLong(text.trim());
                } catch (NumberFormatException ignored) {
                    // Try the next candidate.
                }
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
                if ("true".equalsIgnoreCase(normalized)
                        || "success".equalsIgnoreCase(normalized)
                        || "created".equalsIgnoreCase(normalized)
                        || "queued".equalsIgnoreCase(normalized)
                        || "processing".equalsIgnoreCase(normalized)
                        || "completed".equalsIgnoreCase(normalized)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(normalized)
                        || "failed".equalsIgnoreCase(normalized)
                        || "error".equalsIgnoreCase(normalized)) {
                    return false;
                }
            }
        }
        return null;
    }
}
