package com.rdc.auth.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.rdc.auth.dto.*;
import com.rdc.auth.entity.User;
import com.rdc.auth.repository.UserRepository;
import com.rdc.auth.service.UserService;
import com.rdc.auth.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${google.client.id}")
    private String googleClientId;

    // Alert message for spam folder
    private static final String EMAIL_SPAM_ALERT =
            "Check your Spam or Promotions folder and mark the email as 'Not Spam'.";

    public AuthController(UserService userService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Signup - Create new user account
     * POST /auth/signup
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody LoginRequest req) {
        try {
            userService.createUser(req.getEmail(), req.getPassword());

            Map<String, String> response = new LinkedHashMap<>();
            response.put("message", "Account created. Please check your email to verify.");
            response.put("alert", EMAIL_SPAM_ALERT);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Signup failed: {}", e.getMessage());

            if (e.getMessage().contains("Unable to send")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of(
                                "error", "Unable to send verification email. Please try again later.",
                                "retryable", true
                        ));
            }

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Login - Authenticate and get tokens
     * POST /auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            String accessToken = userService.authenticateAndGetJwt(req.getEmail(), req.getPassword());
            User user = userRepository.findByEmail(req.getEmail()).orElseThrow();

            return ResponseEntity.ok(new TokenResponse(
                    accessToken,
                    jwtUtil.generateRefreshToken(user),
                    jwtUtil.getAccessTokenExpirationSeconds()
            ));

        } catch (IllegalStateException e) {
            // Email not verified
            log.warn("Login failed - email not verified: {}", req.getEmail());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error", "Email not verified",
                            "alert", "Please verify your email before logging in. " + EMAIL_SPAM_ALERT
                    ));

        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
    }

    /**
     * Refresh Token - Get new access token using refresh token
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            if (!jwtUtil.isRefreshToken(refreshToken)) {
                log.warn("Invalid token type - not a refresh token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }

            if (jwtUtil.isTokenExpired(refreshToken)) {
                log.warn("Refresh token expired");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token expired"));
            }

            String email = jwtUtil.getEmailFromToken(refreshToken);

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isEnabled() || !user.isVerified()) {
                log.warn("User account not active: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User account not active"));
            }

            String newAccessToken = jwtUtil.generateToken(user);

            log.info("Token refreshed for user: {}", email);

            return ResponseEntity.ok(Map.of(
                    "accessToken", newAccessToken,
                    "expiresIn", jwtUtil.getAccessTokenExpirationSeconds()
            ));

        } catch (JwtException e) {
            log.error("JWT validation error during refresh: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token refresh failed"));
        }
    }

    /**
     * Verify Email - Called when user clicks verification link
     * GET /auth/verify-email?token=xxx
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        String redirectUrl;
        try {
            userService.verifyAccount(token);
            redirectUrl = frontendUrl + "/verification-success";
            log.info("Email verified successfully");
        } catch (Exception e) {
            log.error("Email verification failed: {}", e.getMessage());
            redirectUrl = frontendUrl + "/verification-failed?error=" + e.getMessage().replace(" ", "_");
        }
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", redirectUrl).build();
    }

    /**
     * Resend Verification Email (Rate Limited)
     * POST /auth/resend-verification
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
            }

            userService.resendVerificationEmail(email);

            log.info("Verification email resent to: {}", email);
            return ResponseEntity.ok(Map.of(
                    "message", "Verification email resent",
                    "alert", EMAIL_SPAM_ALERT
            ));

        } catch (IllegalStateException e) {
            // Rate limit exceeded
            log.warn("Resend rate limit: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("Resend verification failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "message", "If the email exists and is not verified, a verification link has been sent",
                    "alert", EMAIL_SPAM_ALERT
            ));

        } catch (RuntimeException e) {
            log.error("Resend verification email failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "Unable to send email. Please try again later.",
                            "retryable", true
                    ));
        }
    }

    /**
     * Request Password Reset
     * POST /auth/password/request-reset
     */
    @PostMapping("/password/request-reset")
    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> req) {
        try {
            String email = req.get("email");
            if (email != null) {
                userService.createPasswordResetToken(email);
            }
            return ResponseEntity.ok(Map.of(
                    "message", "If an account exists, a reset email has been sent.",
                    "alert", EMAIL_SPAM_ALERT
            ));

        } catch (RuntimeException e) {
            log.error("Password reset email failed: {}", e.getMessage());
            return ResponseEntity.ok(Map.of(
                    "message", "If an account exists, a reset email has been sent.",
                    "alert", EMAIL_SPAM_ALERT
            ));
        }
    }

    /**
     * Reset Password
     * POST /auth/password/reset
     */
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {
        try {
            String token = req.get("token");
            String newPassword = req.get("newPassword");

            if (token == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Token and new password are required"));
            }

            userService.resetPassword(token, newPassword);
            log.info("Password reset successfully");
            return ResponseEntity.ok(Map.of("message", "Password reset successfully."));

        } catch (Exception e) {
            log.error("Password reset failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Google Login
     * POST /auth/google/login
     */
    @PostMapping("/google/login")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> req) {
        try {
            String token = req.get("token");
            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(token);

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String picture = (String) payload.get("picture");

                String jwt = userService.authenticateOrCreateGoogleUser(email, name, picture);
                User user = userRepository.findByEmail(email).orElseThrow();

                log.info("Google login successful for: {}", email);
                return ResponseEntity.ok(new TokenResponse(
                        jwt,
                        jwtUtil.generateRefreshToken(user),
                        jwtUtil.getAccessTokenExpirationSeconds()
                ));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid Google Token"));

        } catch (Exception e) {
            log.error("Google login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Google login failed"));
        }
    }
}