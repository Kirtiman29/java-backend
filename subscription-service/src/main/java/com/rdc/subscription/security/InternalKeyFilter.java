package com.rdc.subscription.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalKeyFilter extends OncePerRequestFilter {

    @Value("${internal.service.key:my-internal-service-key-for-rdc}")
    private String internalServiceKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (!path.startsWith("/api/internal/subscriptions")) {
            filterChain.doFilter(request, response);
            return;
        }

        String headerKey = request.getHeader("X-INTERNAL-KEY");

        if (headerKey == null || !headerKey.equals(internalServiceKey)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                  "status": 403,
                  "error": "Forbidden",
                  "message": "Invalid Internal Key"
                }
                """);
            return;
        }

        filterChain.doFilter(request, response);
    }
}