package com.rdc.admin.controller;

import com.rdc.admin.entity.Admin;
import com.rdc.admin.repository.AdminRepository;
import com.rdc.admin.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AdminRepository adminRepository;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.username, req.password)
        );

        Admin admin = adminRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Admin not found"));

        // ✅ FIX: Use numeric adminId as subject for platform consistency
        String token = jwtUtils.generateToken(String.valueOf(admin.getId()), "ADMIN");

        return ResponseEntity.ok(Map.of(
                "accessToken", token,
                "tokenType", "Bearer",
                "username", admin.getUsername(),
                "roles", List.of("ADMIN")
        ));
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }
}