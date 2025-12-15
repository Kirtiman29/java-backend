package com.rdc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OfferCreateRequest {

    @NotBlank(message = "Offer name is required")
    private String name;

    @NotBlank(message = "Offer code is required")
    private String code;

    // Target: can be null if it's a site-wide offer
    private Long designId;
    private Long categoryId;

    // Discount: one or the other must be set
    private Integer discountPercent;
    private Long discountCents;

    @NotNull(message = "Start date is required")
    private LocalDateTime startsAt;

    private LocalDateTime endsAt;
    private Boolean active = true;
}