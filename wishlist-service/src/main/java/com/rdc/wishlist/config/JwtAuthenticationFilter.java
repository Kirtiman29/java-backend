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

        // 1. If no token, continue the filter chain.
        // Spring Security will check permissions based on your SecurityConfig.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);

            // 2. Validate token without sending an immediate 401 response.
            if (jwtUtil.validateToken(jwt)) {
                String subject = jwtUtil.getSubjectFromToken(jwt);
                Long userId = Long.parseLong(subject);
                String role = jwtUtil.getRoleFromToken(jwt);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                    // FIX: Ensure the "ROLE_" prefix is not duplicated.
                    // This prevents the 403 Forbidden error caused by "ROLE_ROLE_USER".
                    String finalRole = role.toUpperCase().startsWith("ROLE_")
                            ? role.toUpperCase()
                            : "ROLE_" + role.toUpperCase();

                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority(finalRole)
                    );

                    UserPrincipal principal = new UserPrincipal(userId, null, role);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Successfully authenticated user {} with role {}", userId, finalRole);
                }
            } else {
                log.warn("Invalid or expired JWT token provided; proceeding as anonymous user.");
            }

        } catch (Exception e) {
            // Log the error but do not block the request here.
            // SecurityConfig will block it later if the endpoint is not public.
            log.error("JWT Authentication Error: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 3. ALWAYS call doFilter to ensure the request reaches the Controller.
        filterChain.doFilter(request, response);
    }
}