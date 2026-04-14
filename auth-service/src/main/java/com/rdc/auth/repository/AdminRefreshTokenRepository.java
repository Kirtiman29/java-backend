package com.rdc.auth.repository;

import com.rdc.auth.entity.Admin;
import com.rdc.auth.entity.AdminRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRefreshTokenRepository extends JpaRepository<AdminRefreshToken, Long> {
    Optional<AdminRefreshToken> findByToken(String token);
    void deleteAllByAdmin(Admin admin);
    List<AdminRefreshToken> findAllByAdminOrderByExpiryDateDesc(Admin admin);
}
