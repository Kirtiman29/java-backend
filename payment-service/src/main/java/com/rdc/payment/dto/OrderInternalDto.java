package com.rdc.payment.dto;

import lombok.Data;

@Data
public class OrderInternalDto {
    private Long id;
    private Long userId;
    private Long totalPriceCents;
    private String status;
}