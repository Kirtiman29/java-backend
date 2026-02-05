package com.rdc.admin.dto;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class DesignUpdateRequest {
    // Basic Info
    private String title;
    private String description;
    private Long categoryId;
    private String segment;

    // Pricing
    private Long basePriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;

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
    private Boolean premium;

    // ✅ FIXED: Tags list for updating the design_tags table
    private List<String> tags = new ArrayList<>();
}