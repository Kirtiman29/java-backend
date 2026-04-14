package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class BlogResponse {

    private Long id;
    private String slug;
    private String title;
    private String excerpt;
    private String content;
    private String coverAssetUuid;
    private String coverImageUrl;
    private String category;
    private String authorName;
    private Integer readingTimeMinutes;
    private LocalDateTime publishedAt;
    private LocalDate publishedDate;
    private LocalTime publishedTime;
    private Boolean published;
    private Boolean featured;
    private String seoTitle;
    private String seoDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
