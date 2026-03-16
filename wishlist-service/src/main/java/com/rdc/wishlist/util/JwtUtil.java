package com.rdc.wishlist.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private Claims getAllClaimsFromToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getSubjectFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public String getRoleFromToken(String token) {

        Claims claims = getAllClaimsFromToken(token);

        Object role = claims.get("role");

        if (role == null) {
            log.warn("JWT role claim missing");
            return "USER";
        }

        return role.toString();
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {

        Claims claims = getAllClaimsFromToken(token);

        return claimsResolver.apply(claims);
    }

    public boolean validateToken(String token) {

        try {

            Claims claims = getAllClaimsFromToken(token);

            Date expiration = claims.getExpiration();

            return expiration != null && expiration.after(new Date());

        } catch (Exception e) {

            log.error("JWT validation failed: {}", e.getMessage());

            return false;
        }
    }
}