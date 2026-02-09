package com.rdc.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Cart Item Entity
 *
 * Stores DESIGNS (not raw assets) in the cart.
 * Price is stored as a snapshot at the time of adding to cart.
 */
@Entity
@Table(name = "cart_items", indexes = {
        @Index(name = "idx_cart_user_id", columnList = "user_id"),
        @Index(name = "idx_cart_design_id", columnList = "design_id")
})
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

    /**
     * Reference to Design in Admin Service.
     * This replaces the old assetId field.
     */
    @Column(name = "design_id", nullable = false)
    private Long designId;

    /**
     * Asset UUID for preview/download purposes.
     * Fetched from Admin Service when adding to cart.
     */
    @Column(name = "asset_uuid")
    private String assetUuid;

    /**
     * Design title snapshot for display purposes.
     * Prevents extra API calls when listing cart.
     */
    @Column(name = "design_title")
    private String designTitle;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Price snapshot at the time of adding to cart.
     * This is the finalPriceCents from Admin Service.
     * NEVER comes from frontend request.
     */
    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Soft delete flag.
     * Field name is "deleted", column name is "is_deleted".
     */
    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (deleted == null) {
            deleted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Column(name = "design_identifier")
    private String designIdentifier;

}
