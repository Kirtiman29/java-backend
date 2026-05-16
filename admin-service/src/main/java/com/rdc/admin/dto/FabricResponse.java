package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class FabricResponse {

    private Long id;
    private String fabricIdentifier;
    private String slug;
    private String title;
    private String description;

    private Double pricePerMeter;
    private Double pricePerSwatch;
    private Double pricePerQuarter;
    private Double pricePerYard;
    private Double finalPricePerMeter;
    private Double finalPricePerSwatch;
    private Double finalPricePerQuarter;
    private Double finalPricePerYard;
    private Double stockMeters;
    private Integer stockQuantity;

    private String material;
    private Double width;
    private Integer gsm;
    private String length;

    private Long categoryId;
    private Integer discountPercent;
    private Boolean specialOffer;
    private String assetUuid;
    @Builder.Default
    private List<FabricMediaDto> media = new ArrayList<>();
    private Boolean active;
}
