package com.rdc.admin.dto;

// Uses the same structure as CreateRequest, but all fields can be optional
// for partial updates (PUT/PATCH semantics depending on implementation)
import lombok.Data;

@Data
public class CategoryUpdateRequest {
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Boolean active;
    private Integer sortOrder;
}
