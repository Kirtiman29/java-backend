package com.rdc.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssetRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String filename;

    private String contentType;
    private Long sizeBytes;
    private Long priceCents;

    @NotNull
    private Long sellerId;
}
