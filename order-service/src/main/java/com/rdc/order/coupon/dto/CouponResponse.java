package com.rdc.order.coupon.dto;

import com.rdc.order.coupon.enums.AudienceType;
import com.rdc.order.coupon.enums.CouponScope;
import com.rdc.order.coupon.enums.DiscountType;
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
public class CouponResponse {
    private Long id;
    private String code;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal maxDiscountAmount;
    private BigDecimal minOrderAmount;
    private boolean active;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer perUserLimit;
    private AudienceType audienceType;
    private CouponScope couponScope;
    private boolean autoApply;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String message;
}
