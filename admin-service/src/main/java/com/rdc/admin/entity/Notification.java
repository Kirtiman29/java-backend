package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "notifications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notifications_reference_key", columnNames = "reference_key")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private NotificationType type;

    @Column(length = 500)
    private String targetUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean readStatus = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean globalNotification = false;

    @Column(length = 255)
    private String referenceKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime readAt;

    @Column
    private LocalDateTime expiresAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.readStatus == null) {
            this.readStatus = false;
        }
        if (this.globalNotification == null) {
            this.globalNotification = false;
        }
    }
}
