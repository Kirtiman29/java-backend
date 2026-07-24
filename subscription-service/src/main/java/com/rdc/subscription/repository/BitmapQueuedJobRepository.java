package com.rdc.subscription.repository;

import com.rdc.subscription.entity.BitmapQueuedJob;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface BitmapQueuedJobRepository extends JpaRepository<BitmapQueuedJob, Long> {
    Optional<BitmapQueuedJob> findByJobId(Long jobId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BitmapQueuedJob> findByJobIdAndUserId(Long jobId, Long userId);
}
