package com.rdc.asset.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class InternalKeyFilter extends OncePerRequestFilter {

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // ✅ 1. PUBLIC BYPASS (MANDATORY)
        // Ensure images and resumes bypass the key check immediately
        if (path.startsWith("/api/assets/download/") ||
                path.matches("/api/assets/.+/download") ||
                path.startsWith("/api/assets/resume-upload")) {

            filterChain.doFilter(request, response);
            return;
        }

        // 🔒 2. INTERNAL ENDPOINT PROTECTION
        // Apply X-INTERNAL-KEY validation only to internal bridge routes
        if (path.contains("/internal/")) {
            String providedKey = request.getHeader("X-INTERNAL-KEY");

            if (providedKey == null || !providedKey.equals(internalServiceKey)) {
                log.error("❌ BLOCKED: Invalid or missing X-INTERNAL-KEY for path: {}", path);
                // Return SC_FORBIDDEN (403) or SC_UNAUTHORIZED (401)
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid internal service key");
                return;
            }

            log.info("🔑 Authorized internal access: {}", path);
        }

        // 3. Continue to next filter (JWT validation in SecurityConfig)
        filterChain.doFilter(request, response);
    }
}