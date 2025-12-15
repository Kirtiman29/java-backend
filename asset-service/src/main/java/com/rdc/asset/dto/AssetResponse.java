package com.rdc.asset.dto;

import lombok.Data;

@Data
public class AssetResponse {
    private String uuid;
    private String title;
    private String description;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private Long priceCents;
    private Long sellerId;
    private Boolean isPublished;
    private String downloadUrl;
}
