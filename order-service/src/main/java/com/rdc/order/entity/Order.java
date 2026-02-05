package com.rdc.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * Existing field used for base price calculation.
     * Maps to existing physical column 'total_amount_cents'.
     */
    @Column(name = "total_amount_cents", nullable = false)
    private Long totalPriceCents;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    // ✅ INVOICE FIELDS - Explicitly named columns to prevent Hibernate DuplicateMappingException
    @Column(name = "invoice_subtotal_cents")
    private Long subTotalCents;

    @Column(name = "invoice_cgst_cents")
    private Long cgstCents;

    @Column(name = "invoice_sgst_cents")
    private Long sgstCents;

    /**
     * The grand total including taxes.
     * Mapped to 'grand_total_cents' to avoid collision with 'total_amount_cents'.
     */
    @Column(name = "grand_total_cents")
    private Long totalAmountCents;

    @Column(name = "amount_in_words")
    private String amountInWords;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_gstin")
    private String customerGstin;
}