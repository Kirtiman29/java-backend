package com.rdc.auth.service;

import com.rdc.auth.dto.SignupRequest;
import com.rdc.auth.entity.RefreshToken;
import com.rdc.auth.entity.User;
import com.rdc.auth.entity.VerificationToken;
import com.rdc.auth.repository.RefreshTokenRepository;
import com.rdc.auth.repository.UserRepository;
import com.rdc.auth.repository.VerificationTokenRepository;
import com.rdc.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmtpEmailService emailService;
    private final JwtUtil jwtUtil;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${server.port:8081}")
    private String serverPort;

    /* =========================
       REFRESH TOKEN METHODS
       ========================= */

    /**
     * ✅ NEW: Persists new refresh token and revokes old ones (Token Rotation)
     */
    @Override
    @Transactional
    public void saveRefreshToken(User user, String refreshToken) {
        log.info("Rotating refresh token for user: {}", user.getEmail());

        // 🔐 Security: Revoke all existing non-revoked tokens for this user
        // Optimization: Use a custom repository method if available (findByUserAndRevokedFalse)
        refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isRevoked())
                .forEach(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });

        RefreshToken tokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiryDate(Instant.now().plusSeconds(60L * 60 * 24 * 7)) // 7 Days
                .revoked(false)
                .build();

        refreshTokenRepository.save(tokenEntity);
    }

    /**
     * ✅ NEW: Global Logout / Security Reset
     */
    @Override
    @Transactional
    public void revokeAllRefreshTokens(User user) {
        log.info("Revoking all tokens for user: {}", user.getEmail());
        refreshTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isRevoked())
                .forEach(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    /**
     * ✅ UPDATED: Persisted Refresh Logic
     */
    @Override
    @Transactional
    public String refreshAccessToken(String token) {
        if (!"refresh".equals(jwtUtil.getClaim(token, "type"))) {
            throw new RuntimeException("INVALID_TOKEN_TYPE");
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("REFRESH_TOKEN_NOT_FOUND"));

        if (storedToken.isRevoked()) {
            // Security: Potential Token Reuse detected
            revokeAllRefreshTokens(storedToken.getUser());
            throw new RuntimeException("TOKEN_REVOKED");
        }

        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(storedToken);
            throw new RuntimeException("TOKEN_EXPIRED");
        }

        return jwtUtil.generateToken(storedToken.getUser());
    }

    /* =========================
       STANDARD USER METHODS
       ========================= */

    @Override
    @Transactional
    public Map<String, String> createUser(SignupRequest req) {
        log.info("Attempting to create RDC user account: {}", req.getEmail());

        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("USER_EXISTS");
        }

        User newUser = User.builder()
                .email(req.getEmail())
                .displayName(req.getDisplayName())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role("USER")
                .isVerified(false)
                .enabled(true)
                .createdAt(Instant.now())
                .resetCount(0)
                .build();

        newUser = userRepository.save(newUser);

        String token = UUID.randomUUID().toString();
        verificationTokenRepository.save(new VerificationToken(token, newUser, Instant.now().plusSeconds(86400)));

        boolean emailSent = false;
        try {
            String verificationUrl = "http://localhost:" + serverPort + "/auth/verify-email?token=" + token;
            emailSent = emailService.sendVerificationEmail(req.getEmail(), verificationUrl);
        } catch (Exception e) {
            log.error("NON-BLOCKING ERROR: Verification email failed for {}: {}", req.getEmail(), e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("status", "USER_CREATED");
        response.put("emailVerification", emailSent ? "SENT" : "PENDING");
        return response;
    }

    @Override
    public String authenticate(String email, String password, String requiredRole) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_CREDENTIALS"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("USER_DISABLED");
        }

        if (!user.isVerified()) {
            throw new IllegalArgumentException("EMAIL_NOT_VERIFIED");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("INVALID_CREDENTIALS");
        }

        if (!user.getRole().equalsIgnoreCase(requiredRole)) {
            throw new IllegalArgumentException("UNAUTHORIZED_ROLE");
        }

        return jwtUtil.generateToken(user);
    }

    @Override
    @Transactional
    public String authenticateOrCreateGoogleUser(String email, String name, String pictureUrl) {
        log.info("Authenticating RDC Google user: {}", email);
        return processSocialLogin(email, name);
    }

    @Override
    @Transactional
    public String authenticateOrCreateFacebookUser(String email, String name) {
        log.info("Authenticating RDC Facebook user: {}", email);
        return processSocialLogin(email, name);
    }

    private String processSocialLogin(String email, String name) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            log.info("Creating new RDC social user profile: {}", email);
            User newUser = User.builder()
                    .email(email)
                    .passwordHash(UUID.randomUUID().toString())
                    .displayName((name != null && !name.isBlank()) ? name : "RDC User")
                    .role("USER")
                    .createdAt(Instant.now())
                    .isVerified(true)
                    .enabled(true)
                    .resetCount(0)
                    .build();
            return userRepository.save(newUser);
        });

        if (!user.isVerified()) {
            user.setVerified(true);
            user = userRepository.save(user);
        }

        return jwtUtil.generateToken(user);
    }

    @Override
    @Transactional
    public void verifyAccount(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_TOKEN"));

        if (vt.getExpiryDate().isBefore(Instant.now())) {
            verificationTokenRepository.delete(vt);
            throw new IllegalArgumentException("TOKEN_EXPIRED");
        }

        User user = vt.getUser();
        user.setVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(vt);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (user.isVerified()) {
            throw new IllegalArgumentException("ALREADY_VERIFIED");
        }

        verificationTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        verificationTokenRepository.save(new VerificationToken(token, user, Instant.now().plusSeconds(86400)));

        String verificationUrl = "http://localhost:" + serverPort + "/auth/verify-email?token=" + token;
        emailService.sendVerificationEmail(email, verificationUrl);
    }

    @Override
    @Transactional
    public void createPasswordResetToken(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getResetCount() >= 3) {
                log.warn("Password reset limit reached for user: {}", email);
                throw new RuntimeException("RESET_LIMIT_EXCEEDED");
            }

            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiryDate(Instant.now().plusSeconds(600));

            user.setResetCount(user.getResetCount() + 1);
            userRepository.save(user);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(email, resetLink);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_RESET_TOKEN"));

        if (user.getResetTokenExpiryDate() == null || user.getResetTokenExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("TOKEN_EXPIRED");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiryDate(null);
        user.setResetCount(0);
        userRepository.save(user);
    }
}