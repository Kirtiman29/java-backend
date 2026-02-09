package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "design_deletion_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignDeletionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "design_id", nullable = false)
    private Long designId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "deleted_by", nullable = false)
    private String deletedBy;

    @Column(name = "deleted_assets", columnDefinition = "TEXT")
    private String deletedAssetsCsv;

    @Column(name = "design_identifier")
    private String designIdentifier;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;
}
