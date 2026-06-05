package com.rdc.subscription.service;

import com.rdc.subscription.dto.PlanResponse;
import com.rdc.subscription.entity.Plan;
import com.rdc.subscription.enums.BillingCycle;
import com.rdc.subscription.enums.PlanType;
import com.rdc.subscription.repository.PlanRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlanServiceTest {

    private final PlanRepository planRepository = mock(PlanRepository.class);
    private final PlanService planService = new PlanService(planRepository);

    @Test
    void getActivePlansSortsByTypeLimitBillingCycleAndName() {
        Plan aiYearly100 = buildPlan("AI Credits 100", PlanType.AI, BillingCycle.YEARLY, "900.00", 0, 100);
        Plan aiMonthly100 = buildPlan("AI Credits 100", PlanType.AI, BillingCycle.MONTHLY, "900.00", 0, 100);
        Plan aiYearly500 = buildPlan("AI Credits 500", PlanType.AI, BillingCycle.YEARLY, "4000.00", 0, 500);
        Plan designMonthly10 = buildPlan("Designs 10", PlanType.DESIGN, BillingCycle.MONTHLY, "1000.00", 10, 0);

        when(planRepository.findByIsActiveTrue()).thenReturn(List.of(
                aiYearly500,
                designMonthly10,
                aiYearly100,
                aiMonthly100
        ));

        List<PlanResponse> responses = planService.getActivePlans();

        assertThat(responses)
                .extracting(PlanResponse::getName, PlanResponse::getBillingCycle)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("AI Credits 100", BillingCycle.MONTHLY),
                        org.assertj.core.groups.Tuple.tuple("AI Credits 100", BillingCycle.YEARLY),
                        org.assertj.core.groups.Tuple.tuple("AI Credits 500", BillingCycle.YEARLY),
                        org.assertj.core.groups.Tuple.tuple("Designs 10", BillingCycle.MONTHLY)
                );
    }

    private Plan buildPlan(
            String name,
            PlanType planType,
            BillingCycle billingCycle,
            String price,
            int designLimit,
            int creditLimit
    ) {
        return Plan.builder()
                .name(name)
                .planType(planType)
                .billingCycle(billingCycle)
                .price(new BigDecimal(price))
                .designLimit(designLimit)
                .creditLimit(creditLimit)
                .isActive(true)
                .build();
    }
}
