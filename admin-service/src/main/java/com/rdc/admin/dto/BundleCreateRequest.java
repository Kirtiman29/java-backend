package com.rdc.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BundleCreateRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "List of Design IDs is required")
    private List<Long> designIds;

    @NotNull(message = "Price in cents is required")
    @Min(value = 0, message = "Price cannot be negative")
    private Integer priceCents;

    private Boolean active = true;
}