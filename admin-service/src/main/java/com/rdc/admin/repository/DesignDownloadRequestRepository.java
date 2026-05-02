package com.rdc.admin.repository;

import com.rdc.admin.entity.DesignDownloadRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DesignDownloadRequestRepository extends JpaRepository<DesignDownloadRequest, Long> {
    Optional<DesignDownloadRequest> findFirstByUserIdAndDesignIdOrderByCreatedAtDesc(Long userId, Long designId);
    Optional<DesignDownloadRequest> findFirstByDesignIdOrderByCreatedAtDesc(Long designId);
    List<DesignDownloadRequest> findAllByOrderByCreatedAtDesc();
}
