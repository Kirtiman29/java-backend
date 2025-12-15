package com.rdc.admin.repository;

import com.rdc.admin.entity.Bundle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BundleRepository extends JpaRepository<Bundle, Long> {
    // Custom queries for complex bundle queries
}