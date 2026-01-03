package com.rdc.wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistResponse {
    private Long designId;
    private String title;
    private String slug;
    private String assetUuid;
    private Long basePriceCents;      // NEW: Base price
    private Long finalPriceCents;     // NEW: Final price after discount
    private Integer discountPercent;  // NEW: Discount percentage
    private Boolean specialOffer;     // NEW: Special offer flag
}