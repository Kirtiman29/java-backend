package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "designs")
@Data
public class Design {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private Boolean luxury = false;

    // Relations & External IDs
    @ManyToMany
    @JoinTable(
            name = "design_categories",
            joinColumns = @JoinColumn(name = "design_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "design_segments",
            joinColumns = @JoinColumn(name = "design_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "segment")
    private Set<Segment> segments = new HashSet<>();

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

    private String repeatSize;

    private String designType;

    private String imageFormat;

    private String imageType;

    private Integer colorCount;

    private String resolution;

}