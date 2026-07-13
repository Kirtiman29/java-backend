package com.rdc.subscription.repository;

import com.rdc.subscription.entity.BitmapMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BitmapMetadataRepository extends JpaRepository<BitmapMetadata, Long> {
    Optional<BitmapMetadata> findByStoredFilenameAndUserId(String storedFilename, Long userId);
    List<BitmapMetadata> findByUserIdOrderByCreatedAtDesc(Long userId);
}
