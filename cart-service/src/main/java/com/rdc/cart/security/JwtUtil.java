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
     * Auth Service uses: Keys.hmacShaKeyFor(secret.getBytes())
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
     * Auth Service stores role as "USER" (not "ROLE_USER")
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

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

    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            boolean expired = isTokenExpired(token);

            log.debug("Token validation - Subject: {}, Role: {}, Expired: {}",
                    claims.getSubject(),
                    claims.get("role"),
                    expired);

            return !expired;
        } catch (Exception e) {
            log.error("JWT validation failed: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }
}