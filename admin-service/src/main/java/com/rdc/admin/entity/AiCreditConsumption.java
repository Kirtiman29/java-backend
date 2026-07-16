package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "ai_credit_consumptions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ai_credit_consumptions_job",
                        columnNames = {"job_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiCreditConsumption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Column(name = "credits_required", nullable = false)
    private Integer creditsRequired;

    @Column(nullable = false)
    private boolean consumed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public void markConsumed() {
        this.consumed = true;
        this.consumedAt = LocalDateTime.now();
    }
}
