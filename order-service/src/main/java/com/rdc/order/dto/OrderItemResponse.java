package com.rdc.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Order Item Response DTO
 *
 * Uses designId instead of assetId.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long id;
    private Long designId;
    private String assetUuid;
    private String designTitle;
    private Integer quantity;
    private Long priceCents;        // Price per unit
    private Long totalPriceCents; // quantity * priceCents
    private String designIdentifier;

}
