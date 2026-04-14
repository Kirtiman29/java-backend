package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fabric_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FabricMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fabric_id", nullable = false)
    private Long fabricId;

    @Column(name = "asset_uuid", nullable = false, length = 128)
    private String assetUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type")
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_role")
    private MediaRole mediaRole;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
