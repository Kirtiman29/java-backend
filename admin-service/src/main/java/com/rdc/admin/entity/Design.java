package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "designs")
@Data
public class Design {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slug;
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // FIX: Standardized to Long to match DTO and Pricing Service
    private Long basePriceCents;
    private Long finalPriceCents;

    private Boolean specialOffer = false;
    private Integer discountPercent = 0;

    private Boolean active = false;
    private Boolean draft = true;
    private Boolean trending = false;
    private Boolean editorsPick = false;
    private Boolean newArrival = true;

    private Long categoryId;
    private Long assetId;
    private String assetUuid;

    @ElementCollection
    private List<String> tags;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}