package com.rdc.admin.dto;

import com.rdc.admin.entity.DesignDownloadStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DesignDownloadResponse {
    private Long requestId;
    private Long orderId;
    private Long designId;
    private String designIdentifier;
    private String designTitle;
    private DesignDownloadStatus status;
    private Integer remainingDesigns;
    private boolean alreadyRequested;
    private String message;
}
