package com.rdc.subscription.repository;

import com.rdc.subscription.entity.Subscription;
import com.rdc.subscription.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @EntityGraph(attributePaths = {"plan"})
    Optional<Subscription> findFirstByUserIdAndStatusOrderByCreatedAtDesc(Long userId, SubscriptionStatus status);

    @EntityGraph(attributePaths = {"plan"})
    Optional<Subscription> findFirstByUserIdAndStatusAndEndDateAfterOrderByCreatedAtDesc(
            Long userId,
            SubscriptionStatus status,
            LocalDateTime endDate
    );

    @EntityGraph(attributePaths = {"plan"})
    List<Subscription> findByStatusAndEndDateBetween(SubscriptionStatus status, LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"plan"})
    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDateTime endDate);
}
