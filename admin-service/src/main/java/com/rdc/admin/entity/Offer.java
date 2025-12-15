package com.rdc.admin.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "offer")
@Data
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String code;
    private Long designId;   // Optional
    private Long categoryId; // Optional
    private Integer discountPercent;
    private Long discountCents; // Use Long for cents
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private boolean active = true;
}