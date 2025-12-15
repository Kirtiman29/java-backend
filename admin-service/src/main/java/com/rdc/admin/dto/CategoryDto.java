package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class CategoryDto {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private boolean active;
    private int sortOrder;
    private LocalDateTime createdAt;
}
