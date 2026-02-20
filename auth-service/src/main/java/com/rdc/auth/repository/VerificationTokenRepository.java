package com.rdc.auth.repository;

import com.rdc.auth.entity.User;
import com.rdc.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    void deleteByUser(User user);

    Optional<VerificationToken> findByUser(User user);

}