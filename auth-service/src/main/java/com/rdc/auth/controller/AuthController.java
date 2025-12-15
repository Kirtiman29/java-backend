package com.rdc.auth.controller;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.rdc.auth.dto.LoginRequest;
import com.rdc.auth.entity.User;
import com.rdc.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map; // NEW IMPORT

@CrossOrigin(origins = "http://localhost:5173/")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${google.client.id}")
    private String googleClientId;

    public AuthController(UserService userService) {
        this.userService = userService;
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

    // NEW DTO for Facebook/External Access Token
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        try {
            String jwt = userService.authenticateAndGetJwt(req.getEmail(), req.getPassword());
            return ResponseEntity.ok().body(jwt);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // --- GOOGLE LOGIN (Existing) ---
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

                String appJwtToken = userService.authenticateOrCreateGoogleUser(email, name, pictureUrl);
                return ResponseEntity.ok(appJwtToken);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired Google ID Token.");
            }
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Server error during Google token verification.");
        }
    }

    // --- NEW FACEBOOK LOGIN ENDPOINT ---
    @PostMapping("/facebook/login")
    public ResponseEntity<?> facebookLogin(@RequestBody ExternalAccessTokenRequest req) {
        if (req.getAccessToken() == null || req.getAccessToken().isEmpty()) {
            return ResponseEntity.badRequest().body("Access Token is missing.");
        }

        try {
            String jwt = userService.authenticateOrCreateFacebookUser(req.getAccessToken());
            return ResponseEntity.ok(jwt);
        } catch (Exception e) {
            e.printStackTrace();
            // Catching generic Exception for network/API parsing errors from the service layer
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Facebook authentication failed: " + e.getMessage());
        }
    }


    // --- EMAIL VERIFICATION ENDPOINT (Existing) ---
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


    // --- PASSWORD RESET ENDPOINTS (Existing) ---

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