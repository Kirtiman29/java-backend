package com.rdc.admin.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * GENERATE ADMIN TOKEN
     * FIX: Claim name changed from "roles" to "role" to match SecurityConfig.
     */
    public String generateToken(String username, String role) {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        return Jwts.builder()
                .setSubject(username) // sub claim [cite: 1609]
                .claim("role", role)  // FIXED: Must match .setAuthoritiesClaimName("role")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 24 Hours
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}