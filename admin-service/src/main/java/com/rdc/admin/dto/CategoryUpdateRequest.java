package com.rdc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryUpdateRequest {

    private String name;
    private String slug;
    private String imageUuid;
    private String description;
}
