package com.rdc.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving Design information from Admin Service.
 * Used internally by CartService to fetch price and validate design.
 */
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
    private Long finalPriceCents;    // This is the ACTUAL price to use
    private Integer discountPercent;
    private Boolean specialOffer;
    private Long categoryId;
    private Long assetId;
    private String assetUuid;
    private Boolean draft;           // If true, cannot be added to cart
    private Boolean active;          // If false, cannot be added to cart
}
