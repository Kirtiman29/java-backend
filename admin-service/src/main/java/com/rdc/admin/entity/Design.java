package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "designs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Design {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "price_cents", nullable = false)
    private Integer priceCents;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "asset_id")
    private Long assetId;

    @Column(name = "asset_uuid")
    private String assetUuid;

    // The robust ElementCollection mapping:
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "design_tags",
            // Explicitly linking the child table's design_id column to the parent table's id column
            joinColumns = @JoinColumn(name = "design_id", referencedColumnName = "id")
    )
    @Column(name = "tag")
    private List<String> tags;

    @Column(nullable = false)
    private Boolean published = false;

    @Column(nullable = false)
    private Boolean featured = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}