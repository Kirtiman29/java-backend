package com.rdc.admin.dto;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class DesignUpdateRequest {
    // Basic Info
    private String slug;
    private String title;
    private String description;
    private List<String> segments = new ArrayList<>();

    // Pricing
    private Long basePriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;

    private List<Long> categoryIds = new ArrayList<>();

    private String designIdentifier;
    // Industrial Specifications
    private String repeatSize;
    private String designType;
    private String imageFormat;
    private Integer colorCount;
    private String resolution;

    // Media Assets
    private String coverAssetUuid;
    private List<String> galleryUuids;
    private String previewVideoUuid;
    private String downloadTiffUuid;

    // UI & Status Flags
    private Boolean active;
    private Boolean draft;
    private Boolean trending;
    private Boolean editorsPick;
    private Boolean newArrival;
    private Boolean luxury;
    private Boolean subscriptionOnly;

    private List<String> tags = new ArrayList<>();

    private String imageType;
}
