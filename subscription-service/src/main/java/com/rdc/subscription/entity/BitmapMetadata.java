package com.rdc.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bitmap_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitmapMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, unique = true)
    private String storedFilename;

    @Column(name = "bitmap_id")
    private Long bitmapId;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "design_type")
    private String designType;

    private Double confidence;

    @Column(name = "suggested_styles", length = 1000)
    private String suggestedStyles;

    @Column(name = "edge_density_score")
    private Double edgeDensityScore;

    @Column(name = "color_saturation_score")
    private Double colorSaturationScore;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
