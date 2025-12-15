package com.rdc.admin.repository;

import com.rdc.admin.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DesignRepository extends JpaRepository<Design, Long> {

    Optional<Design> findBySlug(String slug);

    // 💡 NEW METHOD: Check for slug uniqueness
    boolean existsBySlug(String slug);
}