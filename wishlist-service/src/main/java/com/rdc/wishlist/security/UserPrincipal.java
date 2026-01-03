package com.rdc.wishlist.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Holds user information extracted from JWT token.
 * Used as the principal in Spring Security context.
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    private Long userId;      // Generated from email hash (same as Cart Service)
    private String email;
    private String role;
}