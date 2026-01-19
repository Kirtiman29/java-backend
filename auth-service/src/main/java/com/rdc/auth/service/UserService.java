package com.rdc.auth.service;

import java.util.Map;

public interface UserService {
    // Returns Map to support non-blocking "emailVerification" status
    Map<String, String> createUser(String email, String password);

    // Unified auth method for ROLE enforcement (USER vs ADMIN)
    String authenticate(String email, String password, String requiredRole);

    void verifyAccount(String token);
    void resendVerificationEmail(String email);
    void createPasswordResetToken(String email);
    void resetPassword(String token, String newPassword);

    // Google OAuth support
    String authenticateOrCreateGoogleUser(String email, String name, String pictureUrl);
}