package com.rdc.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {

    private Long id;
    private Long userId;
    private Long assetId;
    private String assetUuid;
    private Integer quantity;
    private Long priceCents;
}
