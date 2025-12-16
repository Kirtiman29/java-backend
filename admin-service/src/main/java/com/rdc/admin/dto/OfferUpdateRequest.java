package com.rdc.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OfferUpdateRequest {
    private String name;
    private String code;
    private Long designId;
    private Long categoryId;

    @Min(value = 1, message = "Discount percent must be positive")
    @Max(value = 100, message = "Discount cannot exceed 100%")
    private Integer discountPercent;

    @Min(value = 1, message = "Discount amount must be positive")
    private Long discountCents;

    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private Boolean active;
}