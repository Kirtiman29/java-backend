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

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "purchase_type", length = 50)
    private String purchaseType;

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

    @Column(name = "customer_name")
    private String customerName;

    @Column(name="customer_email")
    private String customerEmail;

    @Column(name="customer_phone")
    private String customerPhone;

    @Column(name = "organization_name")
    private String organizationName;

    @Column(name="billing_address_line1")
    private String addressOne;

    @Column(name="billing_address_line2")
    private String addressTwo;

    @Column(name="billing_city")
    private String city;

    @Column(name="billing_state")
    private String billingState;

    @Column(name="billing_pincode")
    private String pincode;

    @Column(name = "billing_country")
    @Builder.Default
    private String country = "India";

    @Column(name = "customer_gstin")
    private String customerGstin;

    @Column(name = "invoice_type")
    private String invoiceType;

    @Column(name = "invoice_subtotal_cents")
    private Long subTotalCents;

    @Column(name = "invoice_cgst_cents")
    private Long cgstCents;

    @Column(name = "invoice_sgst_cents")
    private Long sgstCents;

    @Column(name = "invoice_igst_cents")
    private Long igstCents;

    @Column(name = "grand_total_cents")
    private Long grandTotalCents;

    @Column(name = "amount_in_words")
    private String amountInWords;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "total_amount_cents")
    private Long totalAmountCents;
}
