package com.rdc.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class) // Enables automatic auditing
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 20)
    private String role; // Standardized to match V1__Initial_Auth_Schema.sql

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "reset_token")
    private String resetToken;

    @Builder.Default
    @Column(name = "reset_count", nullable = false)
    private int resetCount = 0;

    @Column(name = "token_expiry_date")
    private Instant resetTokenExpiryDate;

    // Manual setter to ensure consistency with Lombok's boolean generation
    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }
}