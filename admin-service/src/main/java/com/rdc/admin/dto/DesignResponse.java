package com.rdc.admin.dto;

import lombok.AllArgsConstructor; // ADD THIS
import lombok.Data;
import lombok.NoArgsConstructor;  // ADD THIS
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor   // FIX: Allows "new DesignResponse()"
@AllArgsConstructor  // FIX: Required when @Builder or @NoArgsConstructor are used together
public class DesignResponse {
    private Long id;
    private String slug;
    private String title;
    private String description;

    // Pricing
    private Integer basePriceCents;
    private Integer finalPriceCents;
    private Boolean specialOffer;
    private Integer discountPercent;

    // Status
    private Boolean active;
    private Boolean draft;

    // Section Flags
    private Boolean trending;
    private Boolean editorsPick;
    private Boolean newArrival;

    private Long categoryId;
    private Long assetId;
    private String assetUuid;
    private List<String> tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}