package com.rdc.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_media")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AssetMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String uuid;
    private String filename;
    private String contentType;
    private Long sizeBytes;

    @Enumerated(EnumType.STRING)
    private MediaType type;

    private Integer sortOrder;
    private Boolean primaryMedia = false;

    private Long assetId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}