package com.rdc.order.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK to orders.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "asset_uuid")
    private String assetUuid;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
