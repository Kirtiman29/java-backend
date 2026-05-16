package com.rdc.admin.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FabricUpdateRequest {

    private String fabricIdentifier;
    private String slug;
    private String title;
    private String description;

    private Double pricePerMeter;
    private Double pricePerSwatch;
    private Double pricePerQuarter;
    private Double pricePerYard;
    private Double stockMeters;
    private Integer stockQuantity;

    private String material;
    private Double width;
    private Integer gsm;
    private String length;
    private Long categoryId;
    private Integer discountPercent;
    private Boolean specialOffer;
    private String coverAssetUuid;
    private List<String> galleryUuids = new ArrayList<>();

    private Boolean active;
}
