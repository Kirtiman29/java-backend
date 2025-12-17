package com.rdc.admin.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 128, message = "Name must be between 2 and 128 characters")
    private String name;

    private String alias;
    private String image;

    // The ID of the parent category, if any
    private Integer parentId;
}