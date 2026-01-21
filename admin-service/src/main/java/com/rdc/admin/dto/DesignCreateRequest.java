package com.rdc.admin.dto;

import lombok.Data;
import java.util.List;

@Data
public class DesignCreateRequest {
    private String title;
    private String description;
    private Long basePriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;
    private Long categoryId;
    private String segment;

    // ✅ Bulk Media Assets
    private String coverAssetUuid;
    private List<String> galleryUuids;
    private String previewVideoUuid;
    private String downloadTiffUuid;

    // ✅ Status & Section Flags
    private Boolean active = true;
    private Boolean draft = false;
    private Boolean trending = false;
    private Boolean editorsPick = false;
    private Boolean newArrival = true;
    private Boolean premium = false;
}