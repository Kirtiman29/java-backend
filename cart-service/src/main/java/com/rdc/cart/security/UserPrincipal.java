package com.rdc.cart.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Holds user information extracted from JWT token.
 * Used as the principal in Spring Security context.
 *
 * Note: Auth Service JWT contains email and role, not userId.
 * userId must be fetched from database if needed.
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    private String email;
    private String role;
}