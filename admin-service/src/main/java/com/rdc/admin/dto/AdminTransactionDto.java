package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminTransactionDto {
    private Long id;
    private Long designId;
    private Long orderId;
    private Long amountCents;
    private Long adminId;
    private String type; // SALE, REFUND, ADJUSTMENT
    private LocalDateTime timestamp;
}