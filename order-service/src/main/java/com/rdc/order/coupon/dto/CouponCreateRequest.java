package com.rdc.order.coupon.dto;

import com.rdc.order.coupon.enums.AudienceType;
import com.rdc.order.coupon.enums.CouponScope;
import com.rdc.order.coupon.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponCreateRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotNull(message = "Discount type is required")
    private DiscountType discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.01", message = "Discount value must be greater than zero")
    private BigDecimal discountValue;

    @DecimalMin(value = "0.00", inclusive = true, message = "Max discount amount cannot be negative")
    private BigDecimal maxDiscountAmount;

    @DecimalMin(value = "0.00", inclusive = true, message = "Minimum order amount cannot be negative")
    private BigDecimal minOrderAmount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer usageLimit;

    private Integer perUserLimit;

    private AudienceType audienceType;

    private CouponScope couponScope;

    private Boolean autoApply;

    private Boolean active;
}
