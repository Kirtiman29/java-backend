package com.rdc.auth.service;

import com.rdc.auth.dto.SignupRequest;
import java.util.Map;

public interface UserService {
    // Matches the updated SignupRequest logic [cite: 34, 97]
    Map<String, String> createUser(SignupRequest req);

    String authenticate(String email, String password, String requiredRole);

    // OAuth methods from your previous file [cite: 100, 117-119]
    String authenticateOrCreateGoogleUser(String email, String name, String pictureUrl);
    String authenticateOrCreateFacebookUser(String email, String name);

    void verifyAccount(String token);
    void resendVerificationEmail(String email);
    void createPasswordResetToken(String email);
    void resetPassword(String token, String newPassword);
}