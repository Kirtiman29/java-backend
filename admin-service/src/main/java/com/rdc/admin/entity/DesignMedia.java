// src/main/java/com/rdc/admin/entity/DesignMedia.java
package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "design_media")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DesignMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "design_id")
    private Long designId;

    @Column(name = "asset_uuid")
    private String assetUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type")
    private AssetType assetType; // Uses the local Admin-service enum

    @Enumerated(EnumType.STRING)
    @Column(name = "media_role")
    private MediaRole mediaRole; // COVER, GALLERY, PREVIEW_VIDEO, DOWNLOAD

    private Integer sortOrder;
}