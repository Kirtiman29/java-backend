package com.rdc.admin.util;

import com.rdc.admin.dto.DesignMediaDto;
import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.entity.Design;
import com.rdc.admin.entity.DesignMedia;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class DesignMapper {

    /**
     * ✅ Maps Design Entity to DesignResponse DTO.
     * Updated to include the designIdentifier.
     */
    public DesignResponse toResponse(Design design, List<DesignMedia> mediaList) {
        if (design == null) return null;

        return DesignResponse.builder()
                .id(design.getId())
                .designIdentifier(design.getDesignIdentifier()) // ⭐ FIXED: Added missing identifier mapping
                .title(design.getTitle())
                .slug(design.getSlug())
                .description(design.getDescription())
                .basePriceCents(design.getBasePriceCents())
                .finalPriceCents(design.getFinalPriceCents())
                .discountPercent(design.getDiscountPercent())
                .specialOffer(design.getSpecialOffer())
                .categoryId(design.getCategoryId())
                .segment(design.getSegment() != null ? design.getSegment().name() : null)
                .assetUuid(design.getAssetUuid())

                // ✅ Map tags from Entity to Response DTO
                .tags(design.getTags() != null ? new ArrayList<>(design.getTags()) : new ArrayList<>())

                .active(design.getActive())
                .draft(design.getDraft())
                .trending(design.getTrending())
                .editorsPick(design.getEditorsPick())
                .newArrival(design.getNewArrival())
                .premium(design.getPremium())

                .media(mediaList != null ? mediaList.stream().map(this::mapMedia).collect(Collectors.toList()) : List.of())
                .createdAt(design.getCreatedAt())
                .updatedAt(design.getUpdatedAt())
                .build();
    }

    private DesignMediaDto mapMedia(DesignMedia media) {
        return DesignMediaDto.builder()
                .url("http://localhost:8090/api/assets/download/" + media.getAssetUuid())
                .type(media.getAssetType() != null ? media.getAssetType().name() : "IMAGE")
                .role(media.getMediaRole() != null ? media.getMediaRole().name() : "GALLERY")
                .build();
    }
}