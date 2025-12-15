package com.rdc.admin.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "admin_transaction")
@Data
public class AdminTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long designId;
    private Long orderId; // From order-service
    private Long amountCents;
    private Long adminId; // Who uploaded (for revenue-sharing tracking)
    private String type; // SALE, REFUND, ADJUSTMENT
    private LocalDateTime timestamp = LocalDateTime.now();
}