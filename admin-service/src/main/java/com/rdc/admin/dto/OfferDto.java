package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class OfferDto {
    private Long id;
    private String name;
    private String code;
    private Long designId;
    private Long categoryId;
    private Integer discountPercent;
    private Long discountCents;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private boolean active;
}