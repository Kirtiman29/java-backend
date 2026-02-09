package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "designs")
@Data
public class Design {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String slug;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Pricing (Stored in Cents for precision)
    private Long basePriceCents;
    private Long finalPriceCents;

    private Boolean specialOffer = false;
    private Integer discountPercent = 0;

    // Status Flags
    private Boolean active = false;
    private Boolean draft = true;

    // Section Flags
    private Boolean trending = false;
    private Boolean editorsPick = false;
    private Boolean newArrival = true;
    private Boolean premium = false;

    // Relations & External IDs
    private Long categoryId;
    private Long assetId;

    @Column(name = "asset_uuid")
    private String assetUuid;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "design_tags",
            joinColumns = @JoinColumn(name = "design_id")
    )
    @Column(name = "tag_name")
    private List<String> tags = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Segment segment;

    // Timestamps
    @Column(updatable = false)
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


    @Column(name = "design_identifier", nullable = false, unique = true)
    private String designIdentifier;

}