//package com.rdc.admin.repository;
//
//import com.rdc.admin.entity.Design;
//import com.rdc.admin.entity.Segment;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface DesignRepository extends JpaRepository<Design, Long> {
//
//    @Query("SELECT d FROM Design d JOIN d.segments s WHERE s = :segment AND d.active = true")
//    List<Design> findBySegmentAndActiveTrue(@Param("segment") Segment segment);
//
//    List<Design> findByDraftFalseAndActiveTrue();
//
//    boolean existsByDesignIdentifier(String designIdentifier);
//    List<Design> findByDraftFalseAndActiveTrueAndCategories_Id(Long categoryId);
//
//    @Query("""
//SELECT DISTINCT d FROM Design d
//LEFT JOIN d.segments s
//WHERE d.draft = false
//AND d.active = true
//AND (
//LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%'))
//OR LOWER(d.tags) LIKE LOWER(CONCAT('%', :search, '%'))
//OR LOWER(s) LIKE LOWER(CONCAT('%', :search, '%'))
//)
//""")
//    List<Design> searchByTitleOrTags(@Param("search") String search);
//}

package com.rdc.admin.repository;

import com.rdc.admin.entity.Design;
import com.rdc.admin.entity.Segment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignRepository extends JpaRepository<Design, Long> {

    List<Design> findByDraftFalseAndActiveTrue();

    // Change Segment type to String here
    @Query("SELECT d FROM Design d WHERE d.segment = :segment AND d.active = true AND d.draft = false")
    List<Design> findBySegmentAndActiveTrue(@Param("segment") String segment);

    boolean existsByDesignIdentifier(String designIdentifier);

    java.util.Optional<Design> findByDesignIdentifier(String designIdentifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Design d WHERE d.id = :id")
    Optional<Design> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT d FROM Design d JOIN d.categories c WHERE c.id = :categoryId AND d.active = true AND d.draft = false")
    List<Design> findByDraftFalseAndActiveTrueAndCategories_Id(@Param("categoryId") Long categoryId);

    @Query("""
        SELECT d FROM Design d
        WHERE d.draft = false
        AND d.active = true
        AND (
            LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.segment) LIKE LOWER(CONCAT('%', :search, '%'))
        )
    """)
    List<Design> searchByTitleOrTags(@Param("search") String search);
}
