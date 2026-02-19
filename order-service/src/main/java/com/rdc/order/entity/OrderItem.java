package com.rdc.order.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "design_id", nullable = false)
    private Long designId;

    @Column(name = "design_identifier")
    private String designIdentifier;

    @Column(name = "asset_uuid")
    private String assetUuid;

    @Column(name = "design_title")
    private String designTitle;

    @Column(name = "price_cents", nullable = false)
    private Long priceCents;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    public Long getTotalPriceCents() {
        return priceCents * quantity;
    }
}