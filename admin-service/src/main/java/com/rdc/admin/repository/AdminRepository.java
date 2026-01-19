package com.rdc.admin.repository;

import com.rdc.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    // Required for SecurityConfig [cite: 151-153]
    Optional<Admin> findByUsername(String username);
}