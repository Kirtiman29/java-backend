package com.rdc.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class DesignCreateRequest {
    private String title;
    private String description;
    private Long basePriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;
    private Long categoryId;
    private List<String> tags;
    private Long assetId;
    private String assetUuid;

    // Section Flags (optional during creation)
    private Boolean trending;
    private Boolean editorsPick;
    private Boolean newArrival;
    private Boolean premium;  // NEW: Premium section flag
}