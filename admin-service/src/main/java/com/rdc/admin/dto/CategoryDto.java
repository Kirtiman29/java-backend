package com.rdc.admin.dto;

import com.rdc.admin.entity.CategoryScope;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private CategoryScope scope;
    private boolean active;
    private int sortOrder;
    private LocalDateTime createdAt;
}
