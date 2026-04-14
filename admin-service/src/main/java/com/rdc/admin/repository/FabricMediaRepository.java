package com.rdc.admin.repository;

import com.rdc.admin.entity.FabricMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FabricMediaRepository extends JpaRepository<FabricMedia, Long> {
    List<FabricMedia> findByFabricId(Long fabricId);
}
