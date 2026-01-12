package com.rdc.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "users")
@Data
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

    @Column(nullable = false)
    private String role;

    private String displayName;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    private Instant createdAt;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "token_expiry_date")
    private Instant resetTokenExpiryDate;

    // Helper for consistency if Lombok generates setVerified()
    public void setVerified(boolean verified) {
        this.isVerified = verified;
    }
}
