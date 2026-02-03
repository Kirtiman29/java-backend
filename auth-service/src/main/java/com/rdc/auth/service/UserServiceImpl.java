package com.rdc.auth.service;

import com.rdc.auth.dto.SignupRequest;
import com.rdc.auth.entity.User;
import com.rdc.auth.entity.VerificationToken;
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
    private final PasswordEncoder passwordEncoder;
    private final SmtpEmailService emailService;
    private final JwtUtil jwtUtil;

    @Value("${frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${server.port:8081}")
    private String serverPort;

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
                .resetCount(0) // Security: Init reset limit counter
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
                    .passwordHash(UUID.randomUUID().toString()) // Social users have no local pass [cite: 120]
                    .displayName((name != null && !name.isBlank()) ? name : "RDC User")
                    .role("USER")
                    .createdAt(Instant.now())
                    .isVerified(true) // Social accounts are trusted [cite: 121]
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
            // SECURITY: Reset Limit (Prevents spam/brute force on email)
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
        user.setResetCount(0); // Clear counter on success
        userRepository.save(user);
    }
}