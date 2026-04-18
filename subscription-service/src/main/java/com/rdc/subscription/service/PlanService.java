package com.rdc.subscription.service;

import com.rdc.subscription.dto.PlanResponse;
import com.rdc.subscription.entity.Plan;
import com.rdc.subscription.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;

    public List<PlanResponse> getActivePlans() {
        return planRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private PlanResponse mapToResponse(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .planType(plan.getPlanType())
                .billingCycle(plan.getBillingCycle())
                .price(plan.getPrice())
                .designLimit(plan.getDesignLimit())
                .creditLimit(plan.getCreditLimit())
                .build();
    }
}