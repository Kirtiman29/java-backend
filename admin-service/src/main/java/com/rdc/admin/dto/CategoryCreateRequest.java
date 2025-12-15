package com.rdc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Name cannot exceed 255 characters")
    private String name;

    // Slug can be auto-generated later, but allow manual input
    private String slug;

    private String description;
    private String imageUrl;

    private Boolean active = true; // Use Boolean for optional input
    private Integer sortOrder = 0; // Use Integer for optional input
}