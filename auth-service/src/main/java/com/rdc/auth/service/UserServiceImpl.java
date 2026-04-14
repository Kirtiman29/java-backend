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
    private final com.rdc.auth.repository.AdminRepository adminRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final com.rdc.auth.repository.AdminRefreshTokenRepository adminRefreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SmtpEmailService emailService;
    private final JwtUtil jwtUtil;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${app.backend.url}")
    private String backendUrl;

    /* =========================
       REFRESH TOKEN METHODS
       ========================= */

    @Override
    @Transactional
    public void saveRefreshToken(User user, String refreshToken) {
        log.info("Rotating refresh token for user: {}", user.getEmail());

        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserOrderByExpiryDateDesc(user);
        if (activeTokens.size() >= 5) {
            List<RefreshToken> tokensToDelete = activeTokens.subList(4, activeTokens.size());
            refreshTokenRepository.deleteAll(tokensToDelete);
        }

        RefreshToken tokenEntity = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiryDate(Instant.now().plusSeconds(60L * 60 * 24 * 7)) // 7 Days
                .revoked(false)
                .build();

        refreshTokenRepository.save(tokenEntity);
    }
    
    @Override
    @Transactional
    public void saveRefreshToken(com.rdc.auth.entity.Admin admin, String refreshToken) {
        log.info("Rotating refresh token for admin: {}", admin.getEmail());

        List<com.rdc.auth.entity.AdminRefreshToken> activeTokens = adminRefreshTokenRepository.findAllByAdminOrderByExpiryDateDesc(admin);
        if (activeTokens.size() >= 5) {
            List<com.rdc.auth.entity.AdminRefreshToken> tokensToDelete = activeTokens.subList(4, activeTokens.size());
            adminRefreshTokenRepository.deleteAll(tokensToDelete);
        }

        com.rdc.auth.entity.AdminRefreshToken tokenEntity = com.rdc.auth.entity.AdminRefreshToken.builder()
                .admin(admin)
                .token(refreshToken)
                .expiryDate(Instant.now().plusSeconds(60L * 60 * 24 * 7)) // 7 Days
                .revoked(false)
                .build();

        adminRefreshTokenRepository.save(tokenEntity);
    }

    @Override
    @Transactional
    public void revokeAllRefreshTokens(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    @Override
    @Transactional
    public void revokeAllRefreshTokens(com.rdc.auth.entity.Admin admin) {
        adminRefreshTokenRepository.deleteAllByAdmin(admin);
    }

    @Override
    @Transactional
    public String refreshAccessToken(String token, boolean isAdmin) {
        if (!"refresh".equals(jwtUtil.getClaim(token, "type"))) {
            throw new RuntimeException("INVALID_TOKEN_TYPE");
        }

        if (isAdmin) {
            com.rdc.auth.entity.AdminRefreshToken storedToken = adminRefreshTokenRepository.findByToken(token)
                    .orElseThrow(() -> new RuntimeException("REFRESH_TOKEN_NOT_FOUND"));
            if (storedToken.isRevoked()) {
                revokeAllRefreshTokens(storedToken.getAdmin());
                throw new RuntimeException("TOKEN_REVOKED");
            }
            if (storedToken.getExpiryDate().isBefore(Instant.now())) {
                adminRefreshTokenRepository.delete(storedToken);
                throw new RuntimeException("TOKEN_EXPIRED");
            }
            return jwtUtil.generateToken(storedToken.getAdmin());
        }

        RefreshToken storedToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("REFRESH_TOKEN_NOT_FOUND"));

        if (storedToken.isRevoked()) {
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

        Optional<User> existingUserOpt = userRepository.findByEmail(req.getEmail());

        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();

            if (existingUser.isVerified()) {
                throw new RuntimeException("USER_EXISTS");
            }

            log.info("Unverified user {} attempting re-signup. Resending verification.", req.getEmail());
            VerificationToken existingToken =
                    verificationTokenRepository.findByUser(existingUser)
                            .orElse(null);

            String newToken = UUID.randomUUID().toString();
            Instant newExpiry = Instant.now().plusSeconds(300);

            if (existingToken != null) {
                existingToken.setToken(newToken);
                existingToken.setExpiryDate(newExpiry);
                verificationTokenRepository.save(existingToken);
            } else {
                verificationTokenRepository.save(
                        new VerificationToken(newToken, existingUser, newExpiry)
                );
            }

            String verificationUrl = backendUrl + "/api/auth/verify-email?token=" + newToken;
            emailService.sendVerificationEmail(existingUser.getEmail(), verificationUrl);

            Map<String, String> response = new HashMap<>();
            response.put("status", "VERIFICATION_RESENT");
            return response;
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
        verificationTokenRepository.save(new VerificationToken(token, newUser, Instant.now().plusSeconds(300)));

        boolean emailSent = false;
        try {
            String verificationUrl = backendUrl + "/api/auth/verify-email?token=" + token;
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
        if ("ADMIN".equalsIgnoreCase(requiredRole)) {
            com.rdc.auth.entity.Admin admin = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("INVALID_CREDENTIALS"));
            if (!admin.isEnabled()) throw new IllegalArgumentException("USER_DISABLED");
            if (!passwordEncoder.matches(password, admin.getPassword())) {
                throw new IllegalArgumentException("INVALID_CREDENTIALS");
            }
            return jwtUtil.generateToken(admin);
        }

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
        verificationTokenRepository.save(new VerificationToken(token, user, Instant.now().plusSeconds(300)));

        String verificationUrl = backendUrl + "/api/auth/verify-email?token=" + token;
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

    @Override
    @Transactional
    public void generateAndSendOtp(String email, String requiredRole) {
        if ("ADMIN".equalsIgnoreCase(requiredRole)) {
            com.rdc.auth.entity.Admin admin = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
            if (!admin.isEnabled()) throw new IllegalArgumentException("USER_DISABLED");

            String otp = String.format("%06d", new java.util.Random().nextInt(999999));
            admin.setOtp(otp);
            admin.setOtpExpiryDate(Instant.now().plusSeconds(300));
            adminRepository.save(admin);

            emailService.sendOtpEmail(email, otp);
            return;
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("USER_DISABLED");
        }

        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiryDate(Instant.now().plusSeconds(300));
        userRepository.save(user);

        emailService.sendOtpEmail(email, otp);
    }

    @Override
    @Transactional
    public String verifyOtp(String email, String otp, String requiredRole) {
        if ("ADMIN".equalsIgnoreCase(requiredRole)) {
            com.rdc.auth.entity.Admin admin = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
            if (!admin.isEnabled()) throw new IllegalArgumentException("USER_DISABLED");
            if (admin.getOtp() == null || admin.getOtpExpiryDate() == null) throw new IllegalArgumentException("NO_OTP_REQUESTED");
            if (admin.getOtpExpiryDate().isBefore(Instant.now())) throw new IllegalArgumentException("OTP_EXPIRED");
            if (!admin.getOtp().equals(otp)) throw new IllegalArgumentException("INVALID_OTP");
            
            admin.setOtp(null);
            admin.setOtpExpiryDate(null);
            adminRepository.save(admin);
            return jwtUtil.generateToken(admin);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (!user.isEnabled()) {
            throw new IllegalArgumentException("USER_DISABLED");
        }
        if (requiredRole != null && !user.getRole().equalsIgnoreCase(requiredRole)) {
            throw new IllegalArgumentException("UNAUTHORIZED_ROLE");
        }
        if (user.getOtp() == null || user.getOtpExpiryDate() == null) {
            throw new IllegalArgumentException("NO_OTP_REQUESTED");
        }
        if (user.getOtpExpiryDate().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP_EXPIRED");
        }
        if (!user.getOtp().equals(otp)) {
            throw new IllegalArgumentException("INVALID_OTP");
        }

        user.setOtp(null);
        user.setOtpExpiryDate(null);
        userRepository.save(user);

        return jwtUtil.generateToken(user);
    }
}
