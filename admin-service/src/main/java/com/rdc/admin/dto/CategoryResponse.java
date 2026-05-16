package com.rdc.admin.dto;

import com.rdc.admin.entity.CategoryScope;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private CategoryScope scope;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
