package com.rdc.auth.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // You need to configure this in your application.properties or application.yml
    // Example: jwt.secret=a_very_long_and_secure_base64_secret_key_at_least_32_bytes_long
    @Value("${jwt.secret}")
    private String secret;

    // You can set the token expiration time (e.g., 24 hours in milliseconds)
    @Value("${jwt.expiration.ms}")
    private long jwtExpirationMs;

    private Key getSigningKey() {
        // Generates a secure key from your application secret
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generates a JWT token for the authenticated user.
     * @param email The user's email (used as the subject).
     * @param role The user's role (used as a custom claim).
     * @return The generated JWT string.
     */
    public String generateToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role) // Add role as a claim
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // You would typically add validation/parsing methods here too,
    // but this is enough to resolve the immediate compilation error.
}