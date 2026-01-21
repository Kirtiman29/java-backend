// src/main/java/com/rdc/admin/repository/DesignRepository.java
package com.rdc.admin.repository;

import com.rdc.admin.entity.Design;
import com.rdc.admin.entity.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long> {
    // Required for public SEO-friendly URLs [cite: 179, 341-343]
    Optional<Design> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Design> findBySegmentAndActiveTrue(Segment segment);

    List<Design> findByDraftFalseAndActiveTrue();
}