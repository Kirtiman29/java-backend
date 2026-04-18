package com.rdc.subscription.repository;

import com.rdc.subscription.entity.Plan;
import com.rdc.subscription.enums.BillingCycle;
import com.rdc.subscription.enums.PlanType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByIsActiveTrue();
    List<Plan> findByPlanTypeAndIsActiveTrue(PlanType planType);
    Optional<Plan> findByIdAndIsActiveTrue(Long id);
    List<Plan> findByBillingCycleAndIsActiveTrue(BillingCycle billingCycle);
}