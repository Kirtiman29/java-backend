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
}
