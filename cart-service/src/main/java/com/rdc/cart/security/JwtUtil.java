package com.rdc.cart.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * Get signing key - MUST match Auth Service implementation
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Extract email (subject) from JWT
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract role from JWT
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extract expiration from JWT
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse and validate JWT token
     * Uses JJWT 0.11.5 API (same as Auth Service)
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Get token type (access or refresh)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims.get("type", String.class);
        } catch (Exception e) {
            log.debug("No token type claim found - likely old token format");
            return null;
        }
    }

    /**
     * Check if token is an access token
     */
    public boolean isAccessToken(String token) {
        String type = getTokenType(token);
        // Accept tokens without type (backward compatibility) or with type=access
        return type == null || "access".equals(type);
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token
     * Accepts both access tokens and tokens without type (backward compatibility)
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            boolean expired = isTokenExpired(token);
            String tokenType = claims.get("type", String.class);

            log.debug("Token validation - Subject: {}, Role: {}, Type: {}, Expired: {}",
                    claims.getSubject(),
                    claims.get("role"),
                    tokenType != null ? tokenType : "none (old format)",
                    expired);

            // Reject refresh tokens for API calls (they should only be used for /auth/refresh)
            if ("refresh".equals(tokenType)) {
                log.warn("Refresh token used for API call - rejecting");
                return false;
            }

            return !expired;
        } catch (Exception e) {
            log.error("JWT validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
}