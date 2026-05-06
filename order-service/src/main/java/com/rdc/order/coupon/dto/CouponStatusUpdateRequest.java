package com.rdc.order.coupon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponStatusUpdateRequest {

    @NotNull(message = "Active flag is required")
    private Boolean active;
}
