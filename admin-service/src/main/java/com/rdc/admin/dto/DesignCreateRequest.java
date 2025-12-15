package com.rdc.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class DesignCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    // Either assetId or assetUuid must be present for referencing an asset
    private Long assetId;
    private String assetUuid;

    @NotNull(message = "Price in cents is required")
    @Min(value = 1, message = "Price must be greater than zero")
    private Integer priceCents;

    private List<String> tags;
    private Boolean published = false;
}