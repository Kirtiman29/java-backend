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
        config.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        // Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        // Allow ALL headers
        config.setAllowedHeaders(Arrays.asList("*"));

        // Expose headers to frontend
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));

        // Allow credentials
        config.setAllowCredentials(true);

        // Cache preflight for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CORS - must be first and enabled
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Disable CSRF
                .csrf(csrf -> csrf.disable())
                // Stateless session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Allow ALL OPTIONS requests (preflight) - CRITICAL FOR CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Allow actuator
                        .requestMatchers("/actuator/**").permitAll()
                        // Internal endpoints
                        .requestMatchers("/internal/**").permitAll()
                        // Cart endpoints - require USER role
                        .requestMatchers("/api/cart/**").hasRole("USER")
                        // Everything else
                        .anyRequest().authenticated()
                )
                // Add JWT filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}