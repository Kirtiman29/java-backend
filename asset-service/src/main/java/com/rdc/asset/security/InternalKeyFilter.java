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

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (path.startsWith("/api/assets/internal/")) {
            String providedKey = request.getHeader("X-INTERNAL-KEY");

            if (providedKey == null || !providedKey.equals(internalServiceKey)) {
                log.error("❌ BLOCKED: Invalid or missing X-INTERNAL-KEY for path: {}", path);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid internal service key");
                return;
            }
            log.info("🔑 Authorized internal access: {}", path);
        }

        filterChain.doFilter(request, response);
    }
    private boolean isPublicPath(String path) {
        return path.contains("/download") || path.endsWith("/resume-upload");
    }
}