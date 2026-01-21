// src/main/java/com/rdc/admin/repository/DesignMediaRepository.java
package com.rdc.admin.repository;

import com.rdc.admin.entity.DesignMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DesignMediaRepository extends JpaRepository<DesignMedia, Long> {
    // This allows the service to find all media roles (COVER, GALLERY, etc.) for a product
    List<DesignMedia> findByDesignId(Long designId);
}