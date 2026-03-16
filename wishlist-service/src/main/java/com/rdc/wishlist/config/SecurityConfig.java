package com.rdc.wishlist.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Value("${CORS_ALLOWED_ORIGINS:*}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization","Content-Type","Accept","X-Requested-With","X-INTERNAL-KEY"));
        config.setExposedHeaders(Arrays.asList("Authorization","Content-Type"));

        // When allowCredentials is true, AllowedOrigins MUST NOT be "*"
        // If allowedOrigins is "*", Spring will throw an error or default to blocking.
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Always allow OPTIONS for CORS pre-flight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. Public health checks
                        .requestMatchers("/actuator/**").permitAll()

                        // 3. Permit the "check" endpoint for everyone
                        // Using /check/** handles both /check/1 and /check/1/
                        .requestMatchers("/api/wishlist/check/**").permitAll()

                        // 4. Protect main wishlist endpoints
                        // We use both the path and the sub-paths to handle trailing slashes
                        .requestMatchers("/api/wishlist", "/api/wishlist/**").hasRole("USER")

                        // 5. Catch-all
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}