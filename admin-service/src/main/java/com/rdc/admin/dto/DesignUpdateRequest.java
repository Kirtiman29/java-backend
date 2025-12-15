package com.rdc.admin.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO for updating an existing Design.
 * All fields are optional to allow for partial updates (PATCH semantics).
 */
@Data
public class DesignUpdateRequest {

    // String fields - optional
    private String title;
    private String description;

    // Category ID and Asset references - optional
    private Long categoryId;
    private Long assetId;
    private String assetUuid;

    // Price - optional
    private Integer priceCents;

    // Status/Tags - optional
    private List<String> tags;
    private Boolean published;
    private Boolean featured;
}