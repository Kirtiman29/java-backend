package com.rdc.auth.service;

import com.rdc.auth.entity.User;

public interface UserService {

    /**
     * Create new user account and send verification email.
     * FAIL-FAST: If email delivery fails, signup fails.
     */
    User createUser(String email, String password);

    /**
     * Authenticate user and return JWT.
     * @throws IllegalStateException if email not verified
     * @throws IllegalArgumentException if credentials invalid
     */
    String authenticateAndGetJwt(String email, String password);

    /**
     * Verify user account using email token.
     */
    void verifyAccount(String token);

    /**
     * Resend verification email (rate limited).
     * Rate Limits:
     * - 2 minute cooldown between resends
     * - Maximum 3 resends per hour
     */
    void resendVerificationEmail(String email);

    /**
     * Create password reset token and send email.
     */
    void createPasswordResetToken(String email);

    /**
     * Reset password using reset token.
     */
    void resetPassword(String token, String newPassword);

    /**
     * Google OAuth login - creates or updates user.
     * Google users are auto-verified.
     */
    String authenticateOrCreateGoogleUser(String email, String name, String pictureUrl);
}
