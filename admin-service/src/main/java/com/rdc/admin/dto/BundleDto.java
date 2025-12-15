package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BundleDto {
    private Long id;
    private String title;
    private String description;
    private Integer priceCents;
    private List<Long> designIds;
    private boolean active;
    private LocalDateTime createdAt;
}