// src/main/java/com/rdc/admin/util/DesignMapper.java
package com.rdc.admin.util;

import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.entity.Design;
import org.springframework.stereotype.Component;

@Component
public class DesignMapper {

    public DesignResponse toResponse(Design design) {
        if (design == null) return null;

        return DesignResponse.builder()
                .id(design.getId())
                .title(design.getTitle())
                .slug(design.getSlug())
                .description(design.getDescription())
                .basePriceCents(design.getBasePriceCents())
                .finalPriceCents(design.getFinalPriceCents())
                .discountPercent(design.getDiscountPercent())
                .specialOffer(design.getSpecialOffer())
                .categoryId(design.getCategoryId())
                .assetId(design.getAssetId())
                .assetUuid(design.getAssetUuid())
                .active(design.getActive())
                .draft(design.getDraft())
                .trending(design.getTrending())
                .editorsPick(design.getEditorsPick())
                .newArrival(design.getNewArrival())
                .premium(design.getPremium()) // Added Premium flag
                .segment(design.getSegment() != null ? design.getSegment().name() : null) // Map segment to String
                .tags(design.getTags())
                .createdAt(design.getCreatedAt())
                .updatedAt(design.getUpdatedAt())
                .build();
    }
}