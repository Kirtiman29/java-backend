package com.rdc.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cart Item Response DTO
 *
 * Returns design information with price snapshot from Admin Service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Long id;
    private Long userId;
    private Long designId;
    private String assetUuid;      // For preview/download
    private String designTitle;    // Design title for display
    private Integer quantity;
    private Long priceCents;
    private String designIdentifier;
    private Long totalPriceCents;  // quantity * priceCents
}
