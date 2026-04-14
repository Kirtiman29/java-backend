package com.rdc.admin.util;

import com.rdc.admin.dto.CategoryDto;
import com.rdc.admin.dto.DesignMediaDto;
import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.entity.Design;
import com.rdc.admin.entity.DesignMedia;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Component
public class DesignMapper {

    @Value("${service.asset.url}")
    private String assetServiceBaseUrl;

    public DesignResponse toResponse(Design design, List<DesignMedia> mediaList) {
        if (design == null) return null;

        return DesignResponse.builder()
                .id(design.getId())
                .designIdentifier(design.getDesignIdentifier())
                .title(design.getTitle())
                .description(design.getDescription())
                .basePriceCents(design.getBasePriceCents())
                .finalPriceCents(design.getFinalPriceCents())
                .discountPercent(design.getDiscountPercent())
                .specialOffer(design.getSpecialOffer())

                // Industrial Specifications Mapping
                .repeatSize(design.getRepeatSize())
                .designType(design.getDesignType())
                .imageType(design.getImageType())
                .imageFormat(design.getImageFormat())
                .colorCount(design.getColorCount())
                .resolution(design.getResolution())

                .segments(parseSegments(design.getSegment()))

                .assetUuid(design.getAssetUuid())

                .tags(design.getTags() != null ? new ArrayList<>(design.getTags()) : new ArrayList<>())

                .active(design.getActive())
                .draft(design.getDraft())
                .trending(design.getTrending())
                .editorsPick(design.getEditorsPick())
                .newArrival(design.getNewArrival())
                .luxury(design.getLuxury())

                // Categories Mapping
                .categories(
                        design.getCategories() != null ?
                                design.getCategories()
                                        .stream()
                                        .map(cat -> CategoryDto.builder()
                                                .id(cat.getId())
                                                .name(cat.getName())
                                                .description(cat.getDescription())
                                                .imageUrl(cat.getImageUrl())
                                                .build())
                                        .collect(Collectors.toList())
                                : new ArrayList<>()
                )

                .media(
                        mediaList != null
                                ? mediaList.stream().map(this::mapMedia).collect(Collectors.toList())
                                : new ArrayList<>()
                )
                .createdAt(design.getCreatedAt())
                .updatedAt(design.getUpdatedAt())
                .build();
    }

    private DesignMediaDto mapMedia(DesignMedia media) {
        String dynamicUrl = assetServiceBaseUrl + "/api/assets/download/" + media.getAssetUuid();

        return DesignMediaDto.builder()
                .url(dynamicUrl)
                .type(media.getAssetType() != null ? media.getAssetType().name() : "IMAGE")
                .role(media.getMediaRole() != null ? media.getMediaRole().name() : "GALLERY")
                .build();
    }

    private List<String> parseSegments(String segmentValue) {
        if (segmentValue == null || segmentValue.isBlank()) {
            return new ArrayList<>();
        }

        return java.util.Arrays.stream(segmentValue.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }
}
