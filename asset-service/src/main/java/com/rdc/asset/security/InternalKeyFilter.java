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
        // UPDATED: Include the upload path in the internal key check
        if (path.startsWith("/api/assets/internal/") || path.equals("/api/assets/upload")) {
            String providedKey = request.getHeader("X-INTERNAL-KEY");

            if (providedKey == null || !providedKey.equals(internalServiceKey)) {
                log.error("❌ BLOCKED: Invalid or missing X-INTERNAL-KEY for path: {}", path);
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid internal service key");
                return;
            }
            log.info("🔑 Authorized internal access: {}", path);

            /* IMPORTANT: Since we bypassed the JWT check, we need to tell Spring 
               this request is "pre-authenticated" so the SecurityContext is happy.
            */
            if (path.equals("/api/assets/upload")) {
                 // We can proceed. The AssetController will handle the actual logic.
            }
        }

        filterChain.doFilter(request, response);
    }
    private boolean isPublicPath(String path) {
        return path.contains("/download") || path.endsWith("/resume-upload");
    }
}