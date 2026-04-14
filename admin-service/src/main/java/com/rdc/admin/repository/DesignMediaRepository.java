package com.rdc.admin.repository;

import com.rdc.admin.entity.AssetType;
import com.rdc.admin.entity.DesignMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DesignMediaRepository extends JpaRepository<DesignMedia, Long> {

    List<DesignMedia> findByDesignId(Long designId);

    //SINGLE media fetch (cover / preview)
    Optional<DesignMedia> findFirstByDesignIdAndAssetType(Long designId, AssetType assetType);
}
