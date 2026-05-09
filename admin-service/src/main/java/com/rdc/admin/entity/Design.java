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

    @Column(nullable = false)
    private String title;

    @Column(unique = true, length = 255)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "asset_uuid", length = 128)
    private String assetUuid;

    @Column(name = "design_identifier", nullable = false, unique = true, length = 100)
    private String designIdentifier;

    @Column(name = "base_price_cents")
    private Long basePriceCents = 0L;

    @Column(name = "final_price_cents")
    private Long finalPriceCents = 0L;

    private String segment;

    private Boolean active = true;
    private Boolean draft = false;
    private Boolean trending = false;

    @Column(name = "editors_pick")
    private Boolean editorsPick = false;

    @Column(name = "new_arrival")
    private Boolean newArrival = true;

    private Boolean luxury = false;

    // Industrial Specifications matching your SQL and screenshots
    @Column(name = "repeat_size")
    private String repeatSize;

    @Column(name = "design_type")
    private String designType;

    @Column(name = "image_format")
    private String imageFormat;

    @Column(name = "image_type")
    private String imageType;

    @Column(name = "color_count")
    private Integer colorCount;

    private String resolution;

    @Column(name = "special_offer")
    private Boolean specialOffer = false;

    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "subscription_only")
    private Boolean subscriptionOnly = false;

    @Column(name = "download_tiff_uuid", length = 128)
    private String downloadTiffUuid;

    // Many-to-Many Join Table Relationship
    @ManyToMany
    @JoinTable(
            name = "design_categories",
            joinColumns = @JoinColumn(name = "design_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "design_tags", joinColumns = @JoinColumn(name = "design_id"))
    @Column(name = "tag_name")
    private List<String> tags = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
