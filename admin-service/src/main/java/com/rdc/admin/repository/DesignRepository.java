package com.rdc.admin.repository;

import com.rdc.admin.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long> {

    // Required for the unique slug generation logic in DesignService
    boolean existsBySlug(String slug);

    // Section Filtering Logic
    List<Design> findByTrendingTrueAndActiveTrue();
    List<Design> findByEditorsPickTrueAndActiveTrue();
    List<Design> findByNewArrivalTrueAndActiveTrue();

    // Main Public Feed Logic
    List<Design> findByDraftFalseAndActiveTrue();
}