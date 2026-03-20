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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${google.client.id}")
    private String googleClientId;

    private static final String EMAIL_SPAM_ALERT =
            "Check your Spam folder and mark the email as 'Not Spam'.";

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        try {

            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            Long userId = Long.parseLong(principal.getName());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

            return ResponseEntity.ok(Map.of(
                    "name", user.getDisplayName() != null ? user.getDisplayName() : "Industrial User",
                    "email", user.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest req) {
        try {
            String oldRefreshToken = req.getRefreshToken();

            if (!"refresh".equals(jwtUtil.getClaim(oldRefreshToken, "type"))) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "INVALID_TOKEN_TYPE"));
            }

            if (jwtUtil.isTokenExpired(oldRefreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "REFRESH_TOKEN_EXPIRED"));
            }

            String newAccessToken = userService.refreshAccessToken(oldRefreshToken);

            Long userId = Long.parseLong(jwtUtil.getSubjectFromToken(oldRefreshToken));
            User user = userRepository.findById(userId).orElseThrow();
            String newRefreshToken = jwtUtil.generateRefreshToken(user);

            userService.saveRefreshToken(user, newRefreshToken);

            return ResponseEntity.ok(new TokenResponse(
                    newAccessToken,
                    newRefreshToken,
                    jwtUtil.getAccessTokenExpirationSeconds()
            ));
        } catch (Exception e) {
            log.error("Refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "INVALID_REFRESH_TOKEN"));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String idTokenString = request.get("idToken");

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "INVALID_GOOGLE_TOKEN"));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            String accessToken = userService.authenticateOrCreateGoogleUser(email, name, pictureUrl);
            User user = userRepository.findByEmail(email).orElseThrow();

            String refreshToken = jwtUtil.generateRefreshToken(user);
            userService.saveRefreshToken(user, refreshToken);

            return ResponseEntity.ok(new TokenResponse(
                    accessToken,
                    refreshToken,
                    jwtUtil.getAccessTokenExpirationSeconds()
            ));
        } catch (Exception e) {
            log.error("Google auth failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "GOOGLE_AUTH_FAILED"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        try {
            return ResponseEntity.ok(userService.createUser(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        return processLogin(req.getEmail(), req.getPassword(), "USER");
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody LoginRequest req) {
        return processLogin(req.getEmail(), req.getPassword(), "ADMIN");
    }

    @PostMapping("/password/request-reset")
    public ResponseEntity<?> requestReset(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        try {
            if (email != null && !email.isBlank()) {
                userService.createPasswordResetToken(email);
            }
        } catch (Exception e) {
            log.warn("Reset email failed for {}: {}", email, e.getMessage());
            if ("RESET_LIMIT_EXCEEDED".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "RESET_LIMIT_EXCEEDED"));
            }
        }
        return ResponseEntity.ok(Map.of(
                "message", "If an account exists, a reset link has been sent.",
                "alert", EMAIL_SPAM_ALERT
        ));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {
        try {
            userService.resetPassword(req.get("token"), req.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam("token") String token) {
        String redirectUrl;
        try {
            userService.verifyAccount(token);
            redirectUrl = frontendUrl + "/verification-success";
        } catch (Exception e) {
            redirectUrl = frontendUrl + "/verification-failed?error=" + e.getMessage().replace(" ", "_");
        }
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUrl)).build();
    }

    private ResponseEntity<?> processLogin(String email, String password, String role) {
        try {
            String accessToken = userService.authenticate(email, password, role);
            User user = userRepository.findByEmail(email).orElseThrow();

            String refreshToken = jwtUtil.generateRefreshToken(user);
            userService.saveRefreshToken(user, refreshToken);

            return ResponseEntity.ok(new TokenResponse(
                    accessToken,
                    refreshToken,
                    jwtUtil.getAccessTokenExpirationSeconds()
            ));
        } catch (Exception e) {
            log.error("Login attempt failed for {}: {}", email, e.getMessage());

            if ("EMAIL_NOT_VERIFIED".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "EMAIL_NOT_VERIFIED"));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "INVALID_CREDENTIALS"));
        }
    }
}