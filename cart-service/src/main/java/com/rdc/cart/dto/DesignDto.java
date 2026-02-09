package com.rdc.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignDto {
    private Long id;
    private String title;
    private String slug;
    private String description;
    private Long basePriceCents;
    private Long finalPriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;
    private Long categoryId;
    private Long assetId;


    @com.fasterxml.jackson.annotation.JsonProperty("asset_uuid")
    @com.fasterxml.jackson.annotation.JsonAlias("assetUuid")
    private String assetUuid;

    private Boolean draft;
    private Boolean active;
    private String designIdentifier;
}