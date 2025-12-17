package com.rdc.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class DesignCreateRequest {
    private String title;
    private String description;
    private Integer basePriceCents;
    private Boolean specialOffer;
    private Integer discountPercent;
    private Long categoryId;
    private List<String> tags;

    // ADD THESE TWO FIELDS
    private Long assetId;
    private String assetUuid;
}