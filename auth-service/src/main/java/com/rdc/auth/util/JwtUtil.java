package com.rdc.auth.util;

import com.rdc.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private long jwtExpirationMs;

    @Value("${jwt.admin.expiration.ms:${jwt.expiration.ms}}")
    private long adminJwtExpirationMs;

    @Value("${jwt.refresh.expiration.ms}")
    private long refreshExpirationMs;

    private Key signingKey;

    /**
     * ✅ FIXED: Uses Base64 decoding (matches Wishlist Service)
     */
    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
            log.info("🛡️ JWT Signing Key initialized using BASE64 decoding.");
        } catch (Exception e) {
            log.error("❌ Failed to decode JWT secret. Ensure it is valid Base64.");
            throw new IllegalStateException("Invalid JWT secret format", e);
        }
    }

    /* =========================
       TOKEN GENERATION
       ========================= */

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

    public String generateToken(com.rdc.auth.entity.Admin admin) {
        return Jwts.builder()
                .setSubject(String.valueOf(admin.getId()))
                .claim("email", admin.getEmail())
                .claim("role", admin.getRole())
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + adminJwtExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("type", "refresh")
                .setId(UUID.randomUUID().toString()) // 🔥 MOST IMPORTANT FIX
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(com.rdc.auth.entity.Admin admin) {
        return Jwts.builder()
                .setSubject(String.valueOf(admin.getId()))
                .claim("type", "refresh")
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /* =========================
       TOKEN PARSING
       ========================= */

    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("⚠️ Invalid JWT token detected: {}", e.getMessage());
            throw new RuntimeException("INVALID_JWT_TOKEN");
        }
    }

    public String getClaim(String token, String claimName) {
        return parseToken(token).get(claimName, String.class);
    }

    public String getSubjectFromToken(String token) {
        return parseToken(token).getSubject();
    }

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

    public long getAccessTokenExpirationSeconds() {
        return jwtExpirationMs / 1000;
    }

    public long getAdminAccessTokenExpirationSeconds() {
        return adminJwtExpirationMs / 1000;
    }
}
