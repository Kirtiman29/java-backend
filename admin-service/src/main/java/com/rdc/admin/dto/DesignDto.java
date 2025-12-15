package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DesignDto {
    private Long id;
    private String title;
    private String description;
    private Long categoryId;
    private Long assetId;
    private String assetUuid;
    private Integer priceCents;
    private boolean featured;
    private boolean published;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
