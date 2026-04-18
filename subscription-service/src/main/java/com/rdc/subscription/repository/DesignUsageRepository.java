package com.rdc.subscription.repository;

import com.rdc.subscription.entity.DesignUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DesignUsageRepository extends JpaRepository<DesignUsage, Long> {
    Optional<DesignUsage> findFirstByUserIdAndSubscriptionIdOrderByUpdatedAtDesc(Long userId, Long subscriptionId);
}