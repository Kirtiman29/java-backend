package com.rdc.wishlist.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.io.Decoders;
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

    private Key getSigningKey() {
        try {
            // Your .env secret: GiyIPHik/GoBi0wEARevwq9A1x/nNqzzg8v2xICuxFU=
            // This is Base64, so Decoders.BASE64 is correct.
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // Fallback: If for some reason the secret is treated as plain text
            log.warn("JWT secret is not Base64 encoded, falling back to plain text bytes");
            return Keys.hmacShaKeyFor(secret.getBytes());
        }
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getSubjectFromToken(String token) {
        try {
            return getClaimFromToken(token, Claims::getSubject);
        } catch (Exception e) {
            log.error("Could not extract subject from token: {}", e.getMessage());
            return null;
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            // Check for both "role" and "roles" claims commonly used in JWTs
            Object role = claims.get("role");
            if (role == null) role = claims.get("roles");

            return (role != null) ? role.toString() : "USER";
        } catch (Exception e) {
            log.warn("Could not extract role, defaulting to USER");
            return "USER";
        }
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired");
            return false;
        } catch (SignatureException e) {
            log.error("JWT Signature Mismatch! The secret in Wishlist Service does not match Auth Service.");
            return false;
        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}