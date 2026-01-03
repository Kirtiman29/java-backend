package com.rdc.wishlist.config;

import com.rdc.wishlist.security.UserPrincipal;
import com.rdc.wishlist.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // No token provided
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);

            // Validate token
            if (!jwtUtil.validateToken(jwt)) {
                log.warn("Invalid JWT token");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Extract claims from JWT
            String email = jwtUtil.getEmailFromToken(jwt);
            String role = jwtUtil.getRoleFromToken(jwt);

            // Generate userId from email hash (same logic as Cart Service)
            Long userId = generateUserIdFromEmail(email);

            log.debug("JWT validated - Email: {}, Role: {}, UserId: {}", email, role, userId);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Create authority from role
                // Auth Service stores "USER", we need "ROLE_USER" for Spring Security
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
                );

                // Create UserPrincipal with userId, email, role
                UserPrincipal principal = new UserPrincipal(userId, email, role);

                // Create authentication token with UserPrincipal
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {} (userId: {}) with role: ROLE_{}", email, userId, role);
            }

        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Generate stable userId from email hash.
     * Same logic as Cart Service for consistency across services.
     */
    private Long generateUserIdFromEmail(String email) {
        return Math.abs(email.hashCode()) & 0x7FFFFFFFL;
    }
}