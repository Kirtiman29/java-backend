package com.rdc.admin.repository;

import com.rdc.admin.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Used to check for unique slugs during creation
    boolean existsBySlug(String slug);

    // Used to check for unique category names
    boolean existsByName(String name);
}