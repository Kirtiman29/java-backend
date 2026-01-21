package com.rdc.admin.dto;

import lombok.*;
import java.util.List;
import java.time.LocalDateTime;

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
    private String segment;

    // Flags
    private Boolean draft;
    private Boolean active;
    private Boolean trending;
    private Boolean editorsPick;
    private Boolean newArrival;
    private Boolean premium;

    private List<DesignMediaDto> media;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}