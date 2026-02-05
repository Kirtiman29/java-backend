package com.rdc.auth.util;

import com.rdc.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh.expiration.ms}")
    private long refreshExpirationMs;

    private Key signingKey;

    /**
     * ✅ Initialize signing key safely
     * Ensures minimum 256-bit secret for HS256
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException(
                    "JWT secret must be at least 32 characters long"
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /* =========================
       TOKEN GENERATION
       ========================= */

    /**
     * ✅ Generate ACCESS token
     * Subject = numeric userId
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * ✅ Generate REFRESH token
     * Subject = numeric userId
     */
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /* =========================
       TOKEN PARSING & VALIDATION
       ========================= */

    /**
     * ✅ Parse token safely
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("INVALID_JWT_TOKEN");
        }
    }

    /**
     * ✅ Get specific claim (e.g. type)
     */
    public String getClaim(String token, String claimName) {
        return parseToken(token).get(claimName, String.class);
    }

    public String getSubjectFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * ⚠️ Only valid for ACCESS tokens
     */
    public String getEmailFromToken(String token) {
        return parseToken(token).get("email", String.class);
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseToken(token)
                    .getExpiration()
                    .before(new Date());
        } catch (RuntimeException e) {
            return true;
        }
    }

    /* =========================
       UTILITIES
       ========================= */

    public long getAccessTokenExpirationSeconds() {
        return jwtExpirationMs / 1000;
    }
}
