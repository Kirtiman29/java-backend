package com.rdc.auth.service;

import com.rdc.auth.dto.SignupRequest;
import com.rdc.auth.dto.UpdateProfileRequest;
import com.rdc.auth.dto.UserProfileResponse;
import com.rdc.auth.dto.TwoFactorSetupResponse;
import com.rdc.auth.entity.RefreshToken;
import com.rdc.auth.entity.User;
import com.rdc.auth.entity.VerificationToken;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    // We will initialize GoogleAuthenticator locally
    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    // We need RestTemplate to talk to subscription service
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${app.backend.url}")
    private String backendUrl;

    @Value("${service.subscription.url:http://localhost:8094}")
    private String subscriptionServiceUrl;

    @Value("${internal.service.key:ORDER_PAYMENT_BRIDGE_KEY}")
    private String internalServiceKey;

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

        if (user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("2FA_REQUIRED");
        }

        return jwtUtil.generateToken(user);
    }

    @Override
    public String authenticateWith2FA(String email, String password, String code, String requiredRole) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_CREDENTIALS"));

        if (!user.isEnabled()) throw new IllegalArgumentException("USER_DISABLED");
        if (!user.isVerified()) throw new IllegalArgumentException("EMAIL_NOT_VERIFIED");
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("INVALID_CREDENTIALS");
        }
        if (!user.getRole().equalsIgnoreCase(requiredRole)) {
            throw new IllegalArgumentException("UNAUTHORIZED_ROLE");
        }

        if (!user.isTwoFactorEnabled() || user.getTwoFactorSecret() == null) {
            throw new IllegalArgumentException("2FA_NOT_ENABLED");
        }

        int otpCode;
        try {
            otpCode = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("INVALID_2FA_CODE");
        }

        boolean isCodeValid = gAuth.authorize(user.getTwoFactorSecret(), otpCode);
        if (!isCodeValid) {
            throw new IllegalArgumentException("INVALID_2FA_CODE");
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

    /* =========================
       USER PROFILE METHODS
       ========================= */

    @Override
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        UserProfileResponse response = UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .isVerified(user.isVerified())
                .isTwoFactorEnabled(user.isTwoFactorEnabled())
                .build();

        // Fetch subscription and credit data from subscription-service
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String summaryUrl = subscriptionServiceUrl + "/api/internal/subscriptions/users/" + userId + "/summary";
            ResponseEntity<Map> subResponse = restTemplate.exchange(summaryUrl, HttpMethod.GET, entity, Map.class);

            if (subResponse.getStatusCode() == org.springframework.http.HttpStatus.OK && subResponse.getBody() != null) {
                Map<String, Object> body = subResponse.getBody();

                // Parse Subscription
                if (body.containsKey("planName") && body.get("planName") != null) {
                    UserProfileResponse.SubscriptionDetails subDetails = UserProfileResponse.SubscriptionDetails.builder()
                            .planName((String) body.get("planName"))
                            .status((String) body.get("status"))
                            .build();

                    if (body.get("expiresAt") != null) {
                        // Handle potential different numeric formats from JSON parser
                        Object expiresObj = body.get("expiresAt");
                        if (expiresObj instanceof Number) {
                            subDetails.setExpiresAt(((Number) expiresObj).longValue());
                        }
                    }
                    response.setSubscription(subDetails);
                }

                // Parse Credits
                if (body.containsKey("totalCredits") && body.get("totalCredits") != null) {
                    UserProfileResponse.CreditDetails creditDetails = UserProfileResponse.CreditDetails.builder()
                            .totalCredits(((Number) body.get("totalCredits")).intValue())
                            .usedCredits(((Number) body.get("usedCredits")).intValue())
                            .build();
                    response.setCredits(creditDetails);
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch subscription details for user {}: {}", userId, e.getMessage());
            // We don't throw an error here, just return the profile without subscription details
        }

        return response;
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (request.getDisplayName() != null && !request.getDisplayName().trim().isEmpty()) {
            user.setDisplayName(request.getDisplayName().trim());
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() && !user.getEmail().equals(request.getEmail())) {
            // Check if new email is already taken
            if (userRepository.findByEmail(request.getEmail().trim()).isPresent()) {
                throw new IllegalArgumentException("EMAIL_ALREADY_IN_USE");
            }
            user.setEmail(request.getEmail().trim());
            user.setVerified(false); // Require re-verification
            // Trigger verification email here if desired
            resendVerificationEmail(user.getEmail());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            if (request.getOldPassword() == null || !passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("INVALID_OLD_PASSWORD");
            }
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        }

        userRepository.save(user);
        return getUserProfile(userId);
    }

    @Override
    @Transactional
    public TwoFactorSetupResponse setupTwoFactorAuth(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("2FA_ALREADY_ENABLED");
        }

        GoogleAuthenticatorKey key = gAuth.createCredentials();
        String secret = key.getKey();

        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        // Provisioning URI for authenticator apps such as Google Authenticator and Authy.
        String issuer = "RDC E-commerce";
        String accountName = user.getEmail();
        String provisioningUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
                encodeOtpAuthComponent(issuer),
                encodeOtpAuthComponent(accountName),
                secret,
                encodeOtpAuthComponent(issuer));

        return TwoFactorSetupResponse.builder()
                .secret(secret)
                .qrCodeImageUri(generateQrCodeDataUri(provisioningUri))
                .provisioningUri(provisioningUri)
                .build();
    }

    private String generateQrCodeDataUri(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 240, 240);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate 2FA QR code: {}", e.getMessage());
            throw new IllegalStateException("QR_CODE_GENERATION_FAILED", e);
        }
    }

    private String encodeOtpAuthComponent(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @Override
    @Transactional
    public boolean verifyTwoFactorAuth(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (user.getTwoFactorSecret() == null) {
            throw new IllegalArgumentException("2FA_NOT_SETUP");
        }

        int otpCode;
        try {
            otpCode = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return false;
        }

        boolean isCodeValid = gAuth.authorize(user.getTwoFactorSecret(), otpCode);

        if (isCodeValid && !user.isTwoFactorEnabled()) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);
        }

        return isCodeValid;
    }

    @Override
    @Transactional
    public void disableTwoFactorAuth(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (!user.isTwoFactorEnabled()) {
            throw new IllegalArgumentException("2FA_NOT_ENABLED");
        }

        int otpCode;
        try {
            otpCode = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("INVALID_2FA_CODE");
        }

        boolean isCodeValid = gAuth.authorize(user.getTwoFactorSecret(), otpCode);
        if (!isCodeValid) {
            throw new IllegalArgumentException("INVALID_2FA_CODE");
        }

        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null);
        userRepository.save(user);
    }
}
