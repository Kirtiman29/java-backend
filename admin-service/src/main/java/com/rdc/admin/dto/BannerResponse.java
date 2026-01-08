package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class BannerResponse {
    private Long id;
    private String subtitle;
    private String title;
    private String description;
    private String ctaText;
    private String ctaUrl;
    private String backgroundImageUrl; // Fully resolved URL (Port 8090)
    private String theme;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private Integer priority;
}