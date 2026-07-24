package com.rdc.subscription.dto.bitmap;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class BitmapJobResponse {
    private Boolean success;
    private Long jobId;
    private Long userId;
    private String featureName;
    private String workerType;
    private String status;
    private String outputKey;
    private String errorMessage;
    private String message;
    private Boolean queued;
    private Boolean creditsConsumed;
    private Integer creditsRequired;
    private Integer remainingCredits;
    private Map<String, Object> input;
}
