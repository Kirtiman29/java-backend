package com.rdc.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // ✅ JWT APIs do not use CSRF
                .csrf(csrf -> csrf.disable())

                // ✅ FIXED: Proper CORS handling
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ✅ Stateless JWT
                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        // ✅ Public Auth APIs
                        .requestMatchers(
                                "/auth/login",
                                "/auth/signup",
                                "/auth/google",
                                "/auth/facebook",
                                "/auth/refresh",
                                "/auth/verify-email",
                                "/auth/password/**",
                                "/error",
                                "/actuator/**"
                        ).permitAll()

                        // ✅ Internal user metadata (optional public)
                        .requestMatchers("/api/users/**").permitAll()

                        // ✅ Admin-only routes
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ✅ Everything else requires JWT
                        .anyRequest().authenticated()
                )

                // ✅ JWT Resource Server
                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt ->
                                jwt.decoder(jwtDecoder())
                                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    // ✅ JWT Decoder (HMAC SHA256)
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKeySpec secretKey =
                new SecretKeySpec(jwtSecret.getBytes(), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    // ✅ Convert "role" claim → ROLE_USER / ROLE_ADMIN
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    // ✅ FIXED CORS CONFIG (THIS IS CRITICAL)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // ✅ MUST use patterns when allowCredentials = true
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://localhost:*",
                "http://127.0.0.1:*",
                "https://127.0.0.1:*"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-Requested-With"
        ));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ✅ Password Encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ✅ Authentication Manager
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
