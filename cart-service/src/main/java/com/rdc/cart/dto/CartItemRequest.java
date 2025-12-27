package com.rdc.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cart Item Request DTO
 *
 * SECURITY: Frontend NEVER sends price.
 * Price is fetched from Admin/Design Service internally.
 *
 * API Contract:
 * POST /api/cart/items
 * {
 *   "designId": 10,
 *   "quantity": 1
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {

    @NotNull(message = "Design ID is required")
    private Long designId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
