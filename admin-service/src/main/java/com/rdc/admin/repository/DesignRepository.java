package com.rdc.admin.repository;

import com.rdc.admin.entity.Design;
import com.rdc.admin.entity.Segment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long> {

    Optional<Design> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Design> findBySegmentAndActiveTrue(Segment segment);

    List<Design> findByDraftFalseAndActiveTrue();

    boolean existsByDesignIdentifier(String designIdentifier);


    @Query("""
   SELECT DISTINCT d FROM Design d
   LEFT JOIN d.tags t
   WHERE d.active = true
     AND d.draft = false
     AND (
          LOWER(d.title) = LOWER(:search)
          OR LOWER(t) = LOWER(:search)
          OR (LOWER(d.title) LIKE LOWER(CONCAT('% ', :search, ' %')))
     )
""")
    List<Design> searchByTitleOrTags(@Param("search") String search);
}