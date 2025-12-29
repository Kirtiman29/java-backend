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
                filterChain.doFilter(request, response);
                return;
            }

            // Extract claims from JWT
            String email = jwtUtil.extractEmail(jwt);
            String role = jwtUtil.extractRole(jwt);

            log.debug("JWT validated - Email: {}, Role: {}", email, role);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Create authority from role
                // Auth Service stores "USER", we need "ROLE_USER" for Spring Security
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
                );

                // Create authentication token with UserPrincipal
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(email, role),
                        null,
                        authorities
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("Authenticated user: {} with role: ROLE_{}", email, role);
            }

        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}