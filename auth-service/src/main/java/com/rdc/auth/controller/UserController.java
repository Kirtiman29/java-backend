package com.rdc.auth.controller;

import com.rdc.auth.entity.User;
import com.rdc.auth.repository.UserRepository;
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

    @Value("${internal.service.key}")
    private String internalServiceKey;

    /**
     * ✅ INTERNAL BRIDGE ENDPOINT
     * Allows other services (like Order Service) to fetch user email/name.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserMetadata(
            @PathVariable Long id,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {

        // Validate Bridge Key
        if (internalServiceKey == null || !internalServiceKey.equals(key)) {
            log.error("Access Denied: Invalid or missing internal service key for User Metadata");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service key");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Return only necessary fields for the email service
        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "name", user.getDisplayName() != null ? user.getDisplayName() : "Industrial User"
        ));
    }
}