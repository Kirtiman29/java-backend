package com.rdc.auth.service;

import com.rdc.auth.dto.SignupRequest;
import com.rdc.auth.entity.User;

import java.util.Map;

public interface UserService {


    Map<String, String> createUser(SignupRequest req);

    String authenticate(String email, String password, String requiredRole);

    String refreshAccessToken(String refreshToken);


    String authenticateOrCreateGoogleUser(String email, String name, String pictureUrl);

    String authenticateOrCreateFacebookUser(String email, String name);


    void verifyAccount(String token);

    void resendVerificationEmail(String email);

    void createPasswordResetToken(String email);

    void resetPassword(String token, String newPassword);

    void saveRefreshToken(User user, String refreshToken);

    void revokeAllRefreshTokens(User user);
}
