package com.rdc.asset.dto;

import lombok.Data;

@Data
public class AssetDto {

    private String uuid;
    private String title;
    private String description;
    private String filename;
    private String contentType;
    private Long sizeBytes;
    private Long priceCents;
    private Long sellerId;
    private Boolean isPublished;
    private String url;
}
