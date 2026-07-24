package com.rdc.subscription.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "bitmap_queued_jobs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bitmap_queued_jobs_job",
                        columnNames = {"job_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BitmapQueuedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "feature_name", nullable = false, length = 100)
    private String featureName;

    @Column(name = "credits_required", nullable = false)
    private Integer creditsRequired;

    @Column(nullable = false)
    private boolean consumed;

    @Column(length = 40)
    private String status;

    @Column(name = "output_key", length = 2048)
    private String outputKey;

    @Column(name = "error_message", length = 2048)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void markConsumed() {
        this.consumed = true;
        this.consumedAt = LocalDateTime.now();
    }
}
