package com.rdc.admin.dto;

import com.rdc.admin.entity.CategoryScope;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateRequest {

    @NotBlank(message = "Category name is required")
    private String name;
    private String slug;
    private String imageUuid;
    private String description;
    private CategoryScope scope;
}
