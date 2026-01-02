package com.rdc.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class DesignUpdateRequest {
    private String title;
    private String description;
    private Integer basePriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;

    // Status Flags
    private Boolean active;
    private Boolean draft;

    // Section Flags
    private Boolean trending;
    private Boolean editorsPick;
    private Boolean newArrival;
    private Boolean premium;  // NEW: Premium section flag

    private List<String> tags;
    private Long categoryId;
}