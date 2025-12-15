package com.rdc.admin.repository;

import com.rdc.admin.entity.Category; // <-- This import is correct.

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);

}