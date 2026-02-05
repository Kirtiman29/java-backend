package com.rdc.auth.service;

import com.rdc.auth.dto.SignupRequest;
import com.rdc.auth.entity.User;

import java.util.Map;

public interface UserService {

    /* =========================
       AUTHENTICATION
       ========================= */

    Map<String, String> createUser(SignupRequest req);

    String authenticate(String email, String password, String requiredRole);

    /**
     * Uses refresh token to issue new ACCESS token
     */
    String refreshAccessToken(String refreshToken);

    /* =========================
       OAUTH
       ========================= */

    String authenticateOrCreateGoogleUser(String email, String name, String pictureUrl);

    String authenticateOrCreateFacebookUser(String email, String name);

    /* =========================
       ACCOUNT MANAGEMENT
       ========================= */

    void verifyAccount(String token);

    void resendVerificationEmail(String email);

    void createPasswordResetToken(String email);

    void resetPassword(String token, String newPassword);

    /* =========================
       🔥 REFRESH TOKEN MANAGEMENT (FIX)
       ========================= */

    /**
     * Save a refresh token to DB (used on login & OAuth)
     */
    void saveRefreshToken(User user, String refreshToken);

    /**
     * Revoke all refresh tokens for user (used on logout)
     */
    void revokeAllRefreshTokens(User user);
}
