package com.rdc.admin.repository;

import com.rdc.admin.entity.Design;
import com.rdc.admin.entity.Segment; // Ensure this is imported
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long> {

    // Required for unique slug generation [cite: 637]
    boolean existsBySlug(String slug);

    // NEW: Finder for Segment-based filtering
    // This allows the getBySegment API to function
    List<Design> findBySegmentAndActiveTrue(Segment segment);

    // Existing section filters [cite: 638]
    List<Design> findByTrendingTrueAndActiveTrue();
    List<Design> findByEditorsPickTrueAndActiveTrue();
    List<Design> findByNewArrivalTrueAndActiveTrue();

    // Public feed filter
    List<Design> findByDraftFalseAndActiveTrue();
}