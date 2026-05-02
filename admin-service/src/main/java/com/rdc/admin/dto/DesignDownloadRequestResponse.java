package com.rdc.admin.dto;

import com.rdc.admin.entity.DesignDownloadStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DesignDownloadRequestResponse {
    private Long id;
    private Long userId;
    private Long designId;
    private String designIdentifier;
    private String designTitle;
    private Long orderId;
    private DesignDownloadStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime sentAt;
}
