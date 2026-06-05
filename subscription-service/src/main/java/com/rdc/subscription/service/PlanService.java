package com.rdc.subscription.service;

import com.rdc.subscription.dto.PlanResponse;
import com.rdc.subscription.entity.Plan;
import com.rdc.subscription.enums.BillingCycle;
import com.rdc.subscription.enums.PlanType;
import com.rdc.subscription.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    public List<PlanResponse> getActivePlans() {
        return planRepository.findByIsActiveTrue()
                .stream()
                .sorted(Comparator
                        .comparingInt((Plan plan) -> planTypeOrder(plan.getPlanType()))
                        .thenComparingInt(this::primaryLimit)
                        .thenComparingInt(plan -> billingCycleOrder(plan.getBillingCycle()))
                        .thenComparing(Plan::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::mapToResponse)
                .toList();
    }

    private int planTypeOrder(PlanType planType) {
        return switch (planType) {
            case AI -> 0;
            case DESIGN -> 1;
            case COMBO -> 2;
        };
    }

    private int primaryLimit(Plan plan) {
        return switch (plan.getPlanType()) {
            case AI -> plan.getCreditLimit() != null ? plan.getCreditLimit() : 0;
            case DESIGN, COMBO -> plan.getDesignLimit() != null ? plan.getDesignLimit() : 0;
        };
    }

    private int billingCycleOrder(BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> 0;
            case YEARLY -> 1;
        };
    }

    private PlanResponse mapToResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .planType(plan.getPlanType())
                .billingCycle(plan.getBillingCycle())
                .price(plan.getPrice())
                .pricePerDesign(plan.getPricePerDesign())
                .designLimit(plan.getDesignLimit())
                .creditLimit(plan.getCreditLimit())
                .build();
    }
}
