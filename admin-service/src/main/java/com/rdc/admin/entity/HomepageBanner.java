package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "homepage_banners")
@Data
public class HomepageBanner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subtitle;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String description;

    private String ctaText;
    private String ctaUrl;
    private String backgroundImageUrl;

    @Enumerated(EnumType.STRING)
    private BannerTheme theme;

    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean active = false;
    private Integer priority = 0;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}