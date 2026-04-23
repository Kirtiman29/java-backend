package com.rdc.admin.dto.ai;

import lombok.Data;

import java.util.Map;

@Data
public class AiToolRequest {
    private String toolName;
    private String inputUrl;
    private Map<String, Object> params;
}
