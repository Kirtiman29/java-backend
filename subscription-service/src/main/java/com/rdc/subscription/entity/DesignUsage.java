package com.rdc.subscription.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "design_usage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DesignUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    private Integer totalAllowed;

    @Column(nullable = false)
    private Integer usedCount;

    private Integer remainingCount;

    @Column(nullable = false)
    private LocalDateTime periodStart;

    @Column(nullable = false)
    private LocalDateTime periodEnd;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (this.totalAllowed == null) this.totalAllowed = 0;
        if (this.usedCount == null) this.usedCount = 0;
        if (this.remainingCount == null) this.remainingCount = Math.max(this.totalAllowed - this.usedCount, 0);
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
