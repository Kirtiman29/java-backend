package com.rdc.admin.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignResponse {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private Long basePriceCents;
    private Long finalPriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;
    private Long categoryId;
    private List<String> tags;
    private Long assetId;
    private String assetUuid;
    private Boolean draft;
    private Boolean active;
    private Boolean trending;
    private Boolean editorsPick;
    private Boolean newArrival;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}