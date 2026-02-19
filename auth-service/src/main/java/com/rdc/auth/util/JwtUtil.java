package com.rdc.auth.util;

import com.rdc.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration.ms}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh.expiration.ms}")
    private long refreshExpirationMs;

    private Key signingKey;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            log.error("❌ CRITICAL: JWT secret must be at least 32 characters long for industrial security standards.");
            throw new IllegalStateException("JWT secret must be at least 32 characters long");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("🛡️ JWT Signing Key initialized successfully.");
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole()) // Matches ROLE_ prefixing in SecurityConfig
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

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
}