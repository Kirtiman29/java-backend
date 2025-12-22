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

    // Added missing flags to capture from frontend request
    private Boolean active;
    private Boolean draft;
    private Boolean trending;
    private Boolean editorsPick;
    private Boolean newArrival;
}