package com.rdc.auth.service;

import com.rdc.auth.entity.User;
import com.rdc.auth.entity.VerificationToken;
import com.rdc.auth.repository.UserRepository;
import com.rdc.auth.repository.VerificationTokenRepository;
import com.rdc.auth.util.JwtUtil;
import com.rdc.auth.exception.UserAlreadyExistsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;
    private final WebClient webClient; // WebClient is now properly injected

    @Value("${facebook.app.id}") // Fixed line 14 error: Field initialization
    private String fbAppId;

    // NOTE: fbAppSecret is not strictly needed for this user endpoint, but good practice to include it if you needed App access token.

    // Constructor Injection (Fixed lines 29, 45, 53 errors: WebClient Injection)
    @Autowired
    public UserServiceImpl(
            UserRepository userRepository,
            VerificationTokenRepository verificationTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            JwtUtil jwtUtil,
            WebClient.Builder webClientBuilder // Injecting the builder
    ) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.jwtUtil = jwtUtil;
        // Base URL for Facebook Graph API (Fixed line 53 logic)
        this.webClient = webClientBuilder.baseUrl("https://graph.facebook.com/v18.0").build();
    }


    // Helper method to generate and save the token (Same)
    private String generateNewVerificationToken(User user) {
        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusSeconds(24 * 3600);
        VerificationToken verificationToken = new VerificationToken(token, user, expiryDate);
        verificationTokenRepository.save(verificationToken);
        return token;
    }

    // ===============================================
    // 1. SIGNUP LOGIC (Same)
    // ===============================================

    @Override
    @Transactional
    public User createUser(String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserAlreadyExistsException("User with this email already exists.");
        }

        User newUser = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role("USER")
                .createdAt(Instant.now())
                .isVerified(false)
                .enabled(true)
                .build();

        newUser = userRepository.save(newUser);
        String token = generateNewVerificationToken(newUser);

        String verificationLink = "http://localhost:8081/auth/verify-email?token=" + token;
        emailService.sendVerificationEmail(newUser.getEmail(), verificationLink);

        return newUser;
    }

    // ===============================================
    // 2. VERIFICATION LOGIC (Same)
    // ===============================================

    @Override
    @Transactional
    public void verifyAccount(String token) {
        Optional<VerificationToken> tokenOptional = verificationTokenRepository.findByToken(token);

        if (tokenOptional.isEmpty()) {
            throw new IllegalArgumentException("Verification token is invalid or does not exist.");
        }

        VerificationToken verificationToken = tokenOptional.get();
        User user = verificationToken.getUser();

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            verificationTokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Verification token has expired.");
        }

        if (user.isVerified()) {
            verificationTokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Account is already verified.");
        }

        user.setVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);
    }

    // ===============================================
    // 3. AUTHENTICATION / LOGIN (Same)
    // ===============================================

    @Override
    public String authenticateAndGetJwt(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials.");
        }

        if (!user.isVerified()) {
            throw new IllegalStateException("Account is not verified. Please check your email for the verification link.");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole());
    }

    // ===============================================
    // 4. PASSWORD RESET LOGIC (TOKEN CREATION) (Same)
    // ===============================================

    @Override
    @Transactional
    public void createPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        if (user != null) {
            String token = UUID.randomUUID().toString();
            Instant expiryDate = Instant.now().plusSeconds(600);
            user.setResetToken(token);
            user.setResetTokenExpiryDate(expiryDate);
            userRepository.save(user);

            String resetLink = "http://localhost:5173/reset-password?token=" + token;
            emailService.sendResetPasswordEmail(email, resetLink);
        }
    }

    // ===============================================
    // 5. PASSWORD RESET LOGIC (IMPLEMENTATION FIX) (Same)
    // ===============================================

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token."));

        if (user.getResetTokenExpiryDate() != null && user.getResetTokenExpiryDate().isBefore(Instant.now())) {
            user.setResetToken(null);
            user.setResetTokenExpiryDate(null);
            userRepository.save(user);
            throw new IllegalArgumentException("Password reset token has expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiryDate(null);
        userRepository.save(user);
    }

    // ===============================================
    // 6. GOOGLE AUTHENTICATION (Same)
    // ===============================================

    @Override
    public String authenticateOrCreateGoogleUser(String email, String name, String pictureUrl) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!user.isVerified()) {
                user.setVerified(true);
                userRepository.save(user);
            }
            return jwtUtil.generateToken(user.getEmail(), user.getRole());
        }

        User newUser = User.builder()
                .email(email)
                .passwordHash(UUID.randomUUID().toString())
                .displayName(name)
                .role("USER")
                .createdAt(Instant.now())
                .isVerified(true)
                .enabled(true)
                .build();

        userRepository.save(newUser);
        return jwtUtil.generateToken(newUser.getEmail(), newUser.getRole());
    }

    // ===============================================
    // 7. NEW FACEBOOK AUTHENTICATION (Method signature fixed)
    // ===============================================

    @Override // Must be present to match the interface
    @Transactional
    public String authenticateOrCreateFacebookUser(String facebookAccessToken) {
        // 1. Call Facebook Graph API to get user details
        JsonNode userData = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/me")
                        .queryParam("fields", "id,email,name,picture")
                        .queryParam("access_token", facebookAccessToken)
                        .build())
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.createException().flatMap(e -> {
                            throw new RuntimeException("Error verifying token with Facebook: " + e.getMessage());
                        }))
                .bodyToMono(JsonNode.class)
                .block();

        // Basic error checking on returned data
        if (userData == null || !userData.has("email")) {
            throw new IllegalArgumentException("Could not retrieve email from Facebook. Check user permissions or token validity.");
        }

        String email = userData.get("email").asText();
        String name = userData.get("name").asText();

        // 2. Check if user exists in the database
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            if (!user.isVerified()) {
                user.setVerified(true);
                userRepository.save(user);
            }
            return jwtUtil.generateToken(user.getEmail(), user.getRole());
        }

        // 3. Create a new user
        User newUser = User.builder()
                .email(email)
                .passwordHash(UUID.randomUUID().toString())
                .displayName(name)
                .role("USER")
                .createdAt(Instant.now())
                .isVerified(true)
                .enabled(true)
                .build();

        userRepository.save(newUser);
        return jwtUtil.generateToken(newUser.getEmail(), newUser.getRole());
    }
}