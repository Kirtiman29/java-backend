package com.rdc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DesignCreateRequest {
    private String slug;
    private String title;
    private String description;
    private Long basePriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;
    private List<String> segments = new ArrayList<>();

    private List<String> tags = new ArrayList<>();

    private String coverAssetUuid;
    private List<String> galleryUuids;
    private String previewVideoUuid;
    private String downloadTiffUuid;

    private Boolean active = true;
    private Boolean draft = false;
    private Boolean trending = false;
    private Boolean editorsPick = false;
    private Boolean newArrival = true;
    private Boolean luxury = false;
    private Boolean subscriptionOnly = false;

    @NotBlank
    private String designIdentifier;

    private String repeatSize;
    private String imageType;
    private String designType;
    private String imageFormat;
    private Integer colorCount;
    private String resolution;
    private List<Long> categoryIds = new ArrayList<>();

}
