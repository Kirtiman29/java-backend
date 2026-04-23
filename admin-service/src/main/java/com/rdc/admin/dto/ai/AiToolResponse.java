package com.rdc.admin.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AiToolResponse {
    private boolean success;
    private String toolName;
    private String message;
    private String outputUrl;
    private Map<String, Object> outputData;
    private Integer remainingCredits;
}
