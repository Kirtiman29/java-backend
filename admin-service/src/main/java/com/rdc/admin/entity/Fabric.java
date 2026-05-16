package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fabrics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fabric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fabric_identifier", unique = true, length = 100)
    private String fabricIdentifier;

    @Column(unique = true, length = 255)
    private String slug;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double pricePerMeter;
    private Double pricePerSwatch;
    private Double pricePerQuarter;
    private Double pricePerYard;
    private Double stockMeters;
    private Integer stockQuantity;

    private String material;
    private Double width;
    private Integer gsm;

    @Column(name = "fabric_length", length = 100)
    private String length;

    private Long categoryId;

    @Column(name = "asset_uuid", length = 128)
    private String assetUuid;

    private Boolean specialOffer = false;
    private Integer discountPercent = 0;
    private Boolean active = true;
}
