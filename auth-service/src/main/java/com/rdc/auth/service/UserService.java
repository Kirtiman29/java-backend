package com.rdc.auth.service;

import com.rdc.auth.entity.User;
import com.rdc.auth.dto.LoginRequest; // Assuming you have a LoginRequest DTO

public interface UserService {

    // --- Authentication & User Creation ---

    // Used for standard signup
    User createUser(String email, String password);

    // Used for standard login (must now check isVerified)
    String authenticateAndGetJwt(String email, String password);

    // Used for Google login
    String authenticateOrCreateGoogleUser(String email, String name, String pictureUrl);

    // --- Account Verification (NEW METHOD) ---

    /**
     * Finds the verification token, validates it, and sets the user's isVerified flag to true.
     * @param token The unique verification token from the email link.
     * @throws IllegalArgumentException if the token is invalid or expired.
     */
    void verifyAccount(String token); // <<< THE MISSING DECLARATION

    // --- Password Reset ---

    void createPasswordResetToken(String email);

    void resetPassword(String token, String newPassword);

    String authenticateOrCreateFacebookUser(String facebookAccessToken);
}