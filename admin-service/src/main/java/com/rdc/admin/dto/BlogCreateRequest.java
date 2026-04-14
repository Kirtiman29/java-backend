package com.rdc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlogCreateRequest {

    private String slug;

    @NotBlank(message = "Title is required")
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
