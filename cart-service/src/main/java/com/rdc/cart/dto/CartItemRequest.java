// File: src/main/java/com/rdc/cart/dto/CartItemRequest.java
package com.rdc.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    @NotNull
    private Long userId;

    @NotNull
    private Long designId; // Replaced assetId with designId

    @NotNull
    @Min(1)
    private Integer quantity;
}