package com.rdc.cart.security;

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

        // 1. Check for Bearer token [cite: 291]
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);

            // 2. Validate JWT structure and expiration [cite: 312, 313]
            if (!jwtUtil.validateToken(jwt)) {
                log.warn("Invalid JWT token provided");
                filterChain.doFilter(request, response);
                return;
            }

            // 3. Extract claims - Numeric User ID is stored in the Subject claim [cite: 306, 311]
            String userId = jwtUtil.extractEmail(jwt);
            String role = jwtUtil.extractRole(jwt);

            log.debug("JWT validated - UserID: {}, Role: {}", userId, role);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 4. Create authority with ROLE_ prefix for Spring Security [cite: 296, 297]
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
                );

                // 5. CRITICAL FIX: Set numeric userId as the principal [cite: 222, 223]
                // This ensures authentication.getName() returns the ID "2" instead of a UserPrincipal object string
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Successfully authenticated userId: {} with ROLE_{}", userId, role);
            }

        } catch (Exception e) {
            log.error("JWT authentication filter error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}