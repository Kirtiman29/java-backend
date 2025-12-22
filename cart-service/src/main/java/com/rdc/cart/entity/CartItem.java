// File: src/main/java/com/rdc/cart/entity/CartItem.java
package com.rdc.cart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
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

    @Column(name = "design_id", nullable = false) // Updated to designId
    private Long designId;

    @Column(name = "asset_uuid", nullable = false)
    private String assetUuid;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "price_cents", nullable = false) // Backend-controlled field
    private Long priceCents;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;
}