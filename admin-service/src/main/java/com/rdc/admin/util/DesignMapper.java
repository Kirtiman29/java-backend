package com.rdc.admin.util;

import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.entity.Design;

public class DesignMapper {

    private DesignMapper() {
        // Utility class
    }

    /**
     * Converts a Design entity to a DesignResponse DTO.
     */
    public static DesignResponse toResponse(Design design) {
        if (design == null) {
            return null;
        }

        return DesignResponse.builder()
                .id(design.getId())
                .slug(design.getSlug())
                .title(design.getTitle())
                .description(design.getDescription())
                .priceCents(design.getPriceCents())
                .categoryId(design.getCategoryId())
                .assetId(design.getAssetId())
                .assetUuid(design.getAssetUuid())
                .tags(design.getTags())
                .published(design.getPublished())
                .featured(design.getFeatured())
                .createdAt(design.getCreatedAt())
                .updatedAt(design.getUpdatedAt())
                .build();
    }
}