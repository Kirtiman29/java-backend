package com.rdc.auth.service;

import com.rdc.auth.dto.SignupRequest;
import com.rdc.auth.entity.User;

import java.util.Map;

public interface UserService {


    Map<String, String> createUser(SignupRequest req);

    String authenticate(String email, String password, String requiredRole);




    String authenticateOrCreateGoogleUser(String email, String name, String pictureUrl);

    String authenticateOrCreateFacebookUser(String email, String name);


    void verifyAccount(String token);

    void resendVerificationEmail(String email);

    void createPasswordResetToken(String email);

    void resetPassword(String token, String newPassword);

    void saveRefreshToken(User user, String refreshToken);
    void saveRefreshToken(com.rdc.auth.entity.Admin admin, String refreshToken);
    void revokeAllRefreshTokens(User user);
    void revokeAllRefreshTokens(com.rdc.auth.entity.Admin admin);
    String refreshAccessToken(String token, boolean isAdmin);

    void generateAndSendOtp(String email, String requiredRole);

    String verifyOtp(String email, String otp, String requiredRole);
}
