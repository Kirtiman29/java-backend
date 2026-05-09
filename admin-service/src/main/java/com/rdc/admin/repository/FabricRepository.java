package com.rdc.admin.repository;

import com.rdc.admin.entity.Fabric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FabricRepository extends JpaRepository<Fabric, Long> {
    List<Fabric> findByActiveTrue();
    Optional<Fabric> findByFabricIdentifier(String fabricIdentifier);
    Optional<Fabric> findBySlug(String slug);
    boolean existsByFabricIdentifier(String fabricIdentifier);
    boolean existsBySlug(String slug);
    List<Fabric> findAllBySlugIsNull();
    Optional<Fabric> findByTitleIgnoreCase(String title);
}
