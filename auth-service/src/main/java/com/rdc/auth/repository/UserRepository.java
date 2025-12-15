package com.rdc.auth.repository;

import com.rdc.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);

    // --- NEW METHOD FOR PASSWORD RESET ---
    /**
     * Finds a User based on the stored password reset token.
     */
    Optional<User> findByResetToken(String resetToken);
}