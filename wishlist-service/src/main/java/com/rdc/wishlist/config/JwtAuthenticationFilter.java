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

        // 1. If no token, just move to the next filter.
        // Spring Security will handle access control based on SecurityConfig.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);

            // 2. Validate token.
            // FIX: If token is invalid/expired, don't send 401 here.
            // Just don't set the Authentication in the context.
            if (jwtUtil.validateToken(jwt)) {
                String subject = jwtUtil.getSubjectFromToken(jwt);
                Long userId = Long.parseLong(subject);
                String role = jwtUtil.getRoleFromToken(jwt);

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
                    );

                    UserPrincipal principal = new UserPrincipal(userId, null, role);

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } else {
                log.warn("Invalid JWT token provided, proceeding as anonymous user");
            }

        } catch (Exception e) {
            // FIX: Don't write to response. Let the security configuration handle unauthorized access.
            log.error("JWT Authentication Error: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        // 3. ALWAYS call doFilter so the request can reach the Controller
        filterChain.doFilter(request, response);
    }
}