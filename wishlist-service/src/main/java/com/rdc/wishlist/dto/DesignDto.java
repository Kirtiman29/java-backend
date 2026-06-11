package com.rdc.wishlist.dto;

import lombok.Data;

/**
 * DTO for design data from Design/Admin Service
 */
@Data
public class DesignDto {
    private Long id;
    private String designIdentifier;
    private String title;
    private String slug;
    private String assetUuid;
    private Long basePriceCents;
    private Long finalPriceCents;
    private Integer discountPercent;
    private Boolean specialOffer;
    private Boolean active;
    private Boolean draft;
}
