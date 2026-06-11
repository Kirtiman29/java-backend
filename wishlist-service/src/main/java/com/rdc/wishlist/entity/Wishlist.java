package com.rdc.wishlist.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "wishlist", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "design_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wishlist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "design_id", nullable = false)
    private Long designId;

    @Column(name = "design_identifier")
    private String designIdentifier;

    @Column(name = "design_title")
    private String designTitle;

    @Column(name = "asset_uuid")
    private String assetUuid;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private Instant createdAt;
}
