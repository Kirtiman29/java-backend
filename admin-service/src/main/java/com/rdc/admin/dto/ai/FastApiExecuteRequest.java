package com.rdc.admin.dto.ai;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FastApiExecuteRequest {
    private String requestId;
    private Long userId;
    private String toolName;
    private String inputUrl;
    private Map<String, Object> params;
    private String authToken;
}
