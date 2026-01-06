package com.rdc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class DesignCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    @NotNull(message = "Base price is required")
    private Long basePriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;
    @NotNull(message = "Category is required")
    private Long categoryId;
    private List<String> tags;
    private Long assetId;
    private String assetUuid;

    @NotBlank(message = "Segment is required (MENSWEAR, WOMENSWEAR, KIDSWEAR, HOME_INTERIOR)")
    private String segment; // Added for header sections

    private Boolean active;
    private Boolean draft;
    private Boolean trending;
    private Boolean editorsPick;
    private String imageUuid;
    private Boolean newArrival;
    private Boolean premium; // Added for premium section
}