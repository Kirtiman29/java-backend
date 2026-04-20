package com.rdc.auth.controller;

import com.rdc.auth.dto.UpdateProfileRequest;
import com.rdc.auth.dto.UserProfileResponse;
import com.rdc.auth.dto.TwoFactorSetupResponse;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserMetadata(
            @PathVariable Long id,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {

        if (internalServiceKey == null || !internalServiceKey.equals(key)) {
            log.error("Access Denied: Invalid or missing internal service key for User Metadata");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service key");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "name", user.getDisplayName() != null ? user.getDisplayName() : "Industrial User"
        ));
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = extractUserIdFromHeader(token);
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody UpdateProfileRequest request) {
        Long userId = extractUserIdFromHeader(token);
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<TwoFactorSetupResponse> setup2FA(@RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = extractUserIdFromHeader(token);
        return ResponseEntity.ok(userService.setupTwoFactorAuth(userId));
    }

    @PostMapping("/2fa/verify")
    public ResponseEntity<Map<String, Boolean>> verify2FA(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> request) {
        Long userId = extractUserIdFromHeader(token);
        String code = request.get("code");

        if (code == null || code.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code is required");
        }

        boolean isValid = userService.verifyTwoFactorAuth(userId, code);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<Map<String, String>> disable2FA(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> request) {
        Long userId = extractUserIdFromHeader(token);
        String code = request.get("code");

        if (code == null || code.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code is required");
        }

        userService.disableTwoFactorAuth(userId, code);
        return ResponseEntity.ok(Map.of("message", "2FA disabled successfully"));
    }

    private Long extractUserIdFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid token");
        }
        
        try {
            String jwtToken = authHeader.substring(7);
            String subject = jwtUtil.getSubjectFromToken(jwtToken);
            return Long.parseLong(subject);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid JWT token");
        }
    }
}