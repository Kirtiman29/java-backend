package com.rdc.admin.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignResponse {

    private Long id;
    private String slug;
    private String title;
    private String description;
    private Long basePriceCents;
    private Long finalPriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;
    private List<String> segments = new ArrayList<>();
    private String assetUuid;

    //FIXED: Added tags to the response so the frontend can display them
    private List<String> tags;
    private String imageType;
    // Flags
    private Boolean draft;
    private Boolean active;
    private Boolean trending;
    private Boolean editorsPick;
    private Boolean newArrival;
    private Boolean luxury;
    private Boolean subscriptionOnly;

    private List<DesignMediaDto> media;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private String designIdentifier;

    private String repeatSize;
    private String designType;
    private String imageFormat;
    private Integer colorCount;
    private String resolution;
    private List<CategoryDto> categories;

}
