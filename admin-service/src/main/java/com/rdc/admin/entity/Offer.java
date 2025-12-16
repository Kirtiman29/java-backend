package com.rdc.admin.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;

@Entity
@Table(name = "offers") // Renamed table to plural for convention
@Data
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // Added NOT NULL and UNIQUE constraint

    @Column(nullable = false)
    private String name;

    private Long designId;   // Optional
    private Long categoryId; // Optional

    // Using Integer for fields that can be null in the request
    private Integer discountPercent;
    private Long discountCents;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    // FIX: Using primitive boolean with default value to satisfy DB NOT NULL constraint
    // and ensuring new offers are active by default.
    @Column(nullable = false)
    private boolean active = true;
}