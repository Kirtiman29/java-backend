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
    private String segment; // Can be updated

    // Status & Section Flags
    private Boolean active;
    private Boolean draft;
    private Boolean trending;
    private Boolean editorsPick;
    private Boolean newArrival;
    private Boolean premium;

    private List<String> tags;
    private Long categoryId;
}