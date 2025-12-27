package com.rdc.order.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Order Item Entity
 *
 * Represents a single item in an order with LOCKED price from cart.
 */
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_item_design", columnList = "design_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Reference to Design in Admin Service.
     * Replaces the old assetId field.
     */
    @Column(name = "design_id", nullable = false)
    private Long designId;

    /**
     * Asset UUID for download purposes.
     * Copied from cart at order creation.
     */
    @Column(name = "asset_uuid")
    private String assetUuid;

    /**
     * Design title snapshot.
     * Copied from cart at order creation.
     */
    @Column(name = "design_title")
    private String designTitle;

    /**
     * Price per unit in cents.
     * LOCKED at order creation time from cart.
     * Does NOT change if design price changes later.
     */
    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Get total price for this item.
     */
    public Long getTotalPriceCents() {
        return priceCents * quantity;
    }
}
