package com.rdc.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogUpdateRequest {

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
    private Boolean published;
    private Boolean featured;
    private String seoTitle;
    private String seoDescription;
}
