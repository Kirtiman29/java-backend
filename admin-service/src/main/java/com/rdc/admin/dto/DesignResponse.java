package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DesignResponse {
    private Long id;
    private String slug;
    private String title;
    private String description;
    private Integer priceCents;
    private Long categoryId;
    private Long assetId;
    private String assetUuid;
    private List<String> tags;
    private Boolean published;
    private Boolean featured;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}