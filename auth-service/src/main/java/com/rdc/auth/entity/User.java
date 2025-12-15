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

    // Use 'enabled' for overall status, and 'isVerified' for email confirmation
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean isVerified = false; // <<< NEW FIELD for email confirmation

    private Instant createdAt;

    // --- FIELDS FOR PASSWORD RESET ---
    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "token_expiry_date")
    private Instant resetTokenExpiryDate;
    // --- END FIELDS ---
}