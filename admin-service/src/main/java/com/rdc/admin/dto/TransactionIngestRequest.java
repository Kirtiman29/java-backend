package com.rdc.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransactionIngestRequest {

    @NotBlank
    private String transactionId; // Unique ID from the payment service

    @NotNull
    private Long designId;

    @NotNull
    @DecimalMin("0.01")
    private Double amount; // In actual currency units (e.g., USD)

    @NotBlank
    private String currency; // e.g., "USD"

    @NotBlank
    private String status; // e.g., "COMPLETED", "PENDING"

    private String orderId; // Internal order ID if available
}