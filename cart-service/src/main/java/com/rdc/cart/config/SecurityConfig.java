package com.rdc.cart.config;

import com.rdc.cart.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow frontend origins
        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:3000",   // React dev server
                "http://127.0.0.1:5173",
                "http://127.0.0.1:3000"
        ));

        // Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // Allow all headers - especially Authorization
        config.setAllowedHeaders(Arrays.asList("*"));

        // Expose Authorization header to frontend
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS must be first
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF for stateless API
                .csrf(csrf -> csrf.disable())
                // Stateless session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow ALL preflight OPTIONS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Allow actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // Internal endpoints (service-to-service)
                        .requestMatchers("/internal/**").permitAll()
                        // Cart endpoints - require authenticated USER
                        .requestMatchers("/api/cart/**").hasRole("USER")
                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )
                // Add JWT filter BEFORE UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}