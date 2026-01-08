package com.rdc.admin.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class BannerRequest {
    private String subtitle;
    private String title;
    private String description;
    private String ctaText;
    private String ctaUrl;
    private String backgroundImageUuid; // Received from asset-service upload
    private String theme;               // WINTER, SUMMER, DIWALI, SALE, DEFAULT
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active;
    private Integer priority;
}