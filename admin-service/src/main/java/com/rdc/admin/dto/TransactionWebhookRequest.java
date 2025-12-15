package com.rdc.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransactionWebhookRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotNull(message = "Design ID is required")
    private Long designId;

    @NotNull(message = "Amount in cents is required")
    @Min(value = 1, message = "Amount must be positive")
    private Long amountCents;

    @NotNull(message = "Admin ID (uploader) is required")
    private Long adminId;

    @NotBlank(message = "Transaction type is required")
    private String type; // Expected: SALE, REFUND, ADJUSTMENT

    private LocalDateTime timestamp = LocalDateTime.now();
}