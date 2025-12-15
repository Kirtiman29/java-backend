package com.rdc.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
    private Long assetId;

    @NotBlank
    private String assetUuid;

    @NotNull
    private Long priceCents;

    @NotNull
    @Min(1)
    private Integer quantity;
}
