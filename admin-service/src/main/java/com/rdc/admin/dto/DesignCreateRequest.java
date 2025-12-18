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
    private Long categoryId; // Add this
    private List<String> tags;
    private Long assetId;      // Add this
    private String assetUuid;  // Add this
}