package com.rdc.auth.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.rdc.auth.dto.LoginRequest;
import com.rdc.auth.dto.RefreshTokenRequest;
import com.rdc.auth.dto.TokenResponse;
import com.rdc.auth.entity.User;
import com.rdc.auth.repository.UserRepository;
import com.rdc.auth.service.UserService;
import com.rdc.auth.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${google.client.id}")
    private String googleClientId;

    public AuthController(UserService userService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    // --- DTOs ---
    private static class PasswordResetRequest {
        private String token;
        private String newPassword;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    private static class GoogleTokenRequest {
        private String token;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    private static class ExternalAccessTokenRequest {
        private String accessToken;
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    }

    // --- AUTH ENDPOINTS ---

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody LoginRequest req) {
        User u = userService.createUser(req.getEmail(), req.getPassword());
        return ResponseEntity.ok("Account created. Please check your email to verify your account.");
    }

    /**
     * Login - Returns access token + refresh token
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            // Authenticate user (this validates password and checks isVerified)
            String email = req.getEmail();
            String password = req.getPassword();

            // Use existing authentication logic
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid credentials."));

            // Validate password using UserService (reuse existing logic)
            userService.authenticateAndGetJwt(email, password); // This validates password

            // Generate tokens
            String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
            String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

            // Return both tokens
            TokenResponse response = new TokenResponse(
                    accessToken,
                    refreshToken,
                    jwtUtil.getAccessTokenExpirationSeconds()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Refresh access token using refresh token
     * POST /auth/refresh
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            // Validate it's a refresh token
            if (!jwtUtil.isRefreshToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid refresh token"));
            }

            // Check if expired
            if (jwtUtil.isTokenExpired(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Refresh token expired. Please login again."));
            }

            // Extract email from refresh token
            String email = jwtUtil.getEmailFromToken(refreshToken);

            // Find user
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate new tokens
            String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
            String newRefreshToken = jwtUtil.generateRefreshToken(user.getEmail());

            // Return new tokens
            TokenResponse response = new TokenResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtUtil.getAccessTokenExpirationSeconds()
            );

            return ResponseEntity.ok(response);

        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Token refresh failed: " + e.getMessage()));
        }
    }

    // --- GOOGLE LOGIN ---
    @PostMapping("/google/login")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleTokenRequest req) {
        if (req.getToken() == null || req.getToken().isEmpty()) {
            return ResponseEntity.badRequest().body("Token is missing.");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(req.getToken());

            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                String email = payload.getEmail();
                String name = (String) payload.get("name");
                String pictureUrl = (String) payload.get("picture");

                // Create or get user
                userService.authenticateOrCreateGoogleUser(email, name, pictureUrl);

                // Get user for role
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                // Generate tokens
                String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
                String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

                TokenResponse response = new TokenResponse(
                        accessToken,
                        refreshToken,
                        jwtUtil.getAccessTokenExpirationSeconds()
                );

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired Google ID Token.");
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error during Google token verification.");
        }
    }

    // --- FACEBOOK LOGIN ---
    @PostMapping("/facebook/login")
    public ResponseEntity<?> facebookLogin(@RequestBody ExternalAccessTokenRequest req) {
        if (req.getAccessToken() == null || req.getAccessToken().isEmpty()) {
            return ResponseEntity.badRequest().body("Access Token is missing.");
        }

        try {
            // Create or get user
            userService.authenticateOrCreateFacebookUser(req.getAccessToken());

            // Note: We need to get the email from Facebook response
            // For now, return the JWT from service (old behavior)
            // You may need to modify this based on your Facebook implementation
            String jwt = userService.authenticateOrCreateFacebookUser(req.getAccessToken());

            return ResponseEntity.ok(jwt);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Facebook authentication failed: " + e.getMessage());
        }
    }

    // --- EMAIL VERIFICATION ---
    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        String frontendRedirectPath;

        try {
            userService.verifyAccount(token);
            frontendRedirectPath = frontendUrl + "/verification-success";

        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : "invalid_token";
            frontendRedirectPath = frontendUrl + "/verification-failed?error=" + errorMessage.replace(" ", "_");

        } catch (Exception e) {
            e.printStackTrace();
            frontendRedirectPath = frontendUrl + "/verification-failed?error=server_error";
        }

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header("Location", frontendRedirectPath)
                .build();
    }

    // --- PASSWORD RESET ---
    @PostMapping("/password/request-reset")
    public ResponseEntity<?> requestPasswordReset(@RequestBody LoginRequest req) {
        try {
            userService.createPasswordResetToken(req.getEmail());
            return ResponseEntity.ok().body("If an account exists, a password reset email has been sent.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok().body("If an account exists, a password reset email has been sent.");
        }
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest req) {
        if (req.getToken() == null || req.getNewPassword() == null) {
            return ResponseEntity.badRequest().body("Missing token or new password.");
        }

        try {
            userService.resetPassword(req.getToken(), req.getNewPassword());
            return ResponseEntity.ok().body("Password has been successfully reset.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
}