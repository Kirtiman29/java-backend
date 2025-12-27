package com.rdc.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for receiving cart items from Cart Service.
 * Maps to CartItemResponse from cart-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemDto {
    private Long id;
    private Long userId;
    private Long designId;
    private String assetUuid;
    private String designTitle;
    private Integer quantity;
    private Long priceCents;
    private Long totalPriceCents;
}
