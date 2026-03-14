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

    @Query("SELECT d FROM Design d JOIN d.segments s WHERE s = :segment AND d.active = true")
    List<Design> findBySegmentAndActiveTrue(@Param("segment") Segment segment);

    List<Design> findByDraftFalseAndActiveTrue();

    boolean existsByDesignIdentifier(String designIdentifier);
    List<Design> findByDraftFalseAndActiveTrueAndCategories_Id(Long categoryId);

    @Query("""
SELECT DISTINCT d FROM Design d
LEFT JOIN d.segments s
WHERE d.draft = false
AND d.active = true
AND (
LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%'))
OR LOWER(d.tags) LIKE LOWER(CONCAT('%', :search, '%'))
OR LOWER(s) LIKE LOWER(CONCAT('%', :search, '%'))
)
""")
    List<Design> searchByTitleOrTags(@Param("search") String search);
}