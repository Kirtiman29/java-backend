package com.rdc.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Global CORS configuration to allow the Admin UI to access the API.
     * We use allowedOriginPatterns("*") instead of allowedOrigins("*")
     * because we have allowCredentials(true) set.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/admin/**") // Apply to all admin API endpoints
                .allowedOriginPatterns("*") // FIX: Changed from allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}