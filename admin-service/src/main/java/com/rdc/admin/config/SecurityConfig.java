package com.rdc.admin.config;

import com.rdc.admin.repository.AdminRepository;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${CORS_ALLOWED_ORIGINS}")
    private String allowedOrigins;

    /* ================================
       JWT DECODER
    ================================ */
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /* ================================
       JWT ROLE MAPPING
    ================================ */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter converter = new JwtGrantedAuthoritiesConverter();
        converter.setAuthorityPrefix("ROLE_");
        converter.setAuthoritiesClaimName("role");

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(converter);

        return jwtConverter;
    }

    /* ================================
       SECURITY FILTER CHAIN
    ================================ */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth

                        /* ALWAYS ALLOW PREFLIGHT */
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        /* INTERNAL MICROSERVICE COMMUNICATION */
                        .requestMatchers("/api/internal/**").permitAll()

                        /* PUBLIC STORE FRONT */
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/categories/public").permitAll()

                        /* CONTACT FORM */
                        .requestMatchers("/api/public/contact/**").permitAll()

                        /* SEO SITEMAPS */
                        .requestMatchers(
                                "/sitemap.xml",
                                "/sitemap-pages.xml",
                                "/sitemap-products.xml",
                                "/sitemap-categories.xml"
                        ).permitAll()

                        /* ADMIN LOGIN */
                        .requestMatchers("/api/admin/login").permitAll()
                        
                        /* AI TOOLS API */
                        .requestMatchers("/api/ai/use").authenticated()

                        /* ADMIN PROTECTED APIs */
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        /* EVERYTHING ELSE REQUIRES AUTH */
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth ->
                        oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    /* ================================
       AUTH MANAGER
    ================================ */
    @Bean
    public AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /* ================================
       ADMIN USER LOADER
    ================================ */
    @Bean
    public UserDetailsService userDetailsService(AdminRepository adminRepository) {

        return username -> adminRepository.findByUsername(username)
                .map(admin -> new org.springframework.security.core.userdetails.User(
                        admin.getUsername(),
                        admin.getPassword(),
                        admin.isEnabled(),
                        true,
                        true,
                        true,
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + admin.getRole())
                        )
                ))
                .orElseThrow(() ->
                        new UsernameNotFoundException("Admin not found: " + username)
                );
    }

    /* ================================
       PASSWORD ENCODER
    ================================ */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* ================================
       CORS CONFIGURATION
    ================================ */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        config.setAllowedOrigins(origins);

        config.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        );

        config.setAllowedHeaders(
                Arrays.asList(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "X-Requested-With",
                        "X-INTERNAL-KEY"
                )
        );

        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
