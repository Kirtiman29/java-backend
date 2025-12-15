package com.rdc.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "asset_uuid", nullable = false)
    private String assetUuid;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // field ka naam "deleted" rakha hai, column ka naam is_deleted
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;
}
