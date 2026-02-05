package com.rdc.auth.repository;

import com.rdc.auth.entity.RefreshToken;
import com.rdc.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    // ✅ Logout from all devices
    void deleteAllByUser(User user);
}
