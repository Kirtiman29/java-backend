package com.rdc.subscription.service;

import com.rdc.subscription.entity.Plan;
import com.rdc.subscription.enums.BillingCycle;
import com.rdc.subscription.enums.PlanType;
import com.rdc.subscription.repository.PlanRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PredefinedPlanCatalogInitializer implements ApplicationRunner {

    private static final List<PredefinedPlanDefinition> PREDEFINED_PLANS = List.of(
            PredefinedPlanDefinition.ai("AI Credits 50", BillingCycle.MONTHLY, "500.00", 50),
            PredefinedPlanDefinition.ai("AI Credits 100", BillingCycle.MONTHLY, "900.00", 100),
            PredefinedPlanDefinition.ai("AI Credits 500", BillingCycle.MONTHLY, "4000.00", 500),
            PredefinedPlanDefinition.ai("AI Credits 1000", BillingCycle.MONTHLY, "7000.00", 1000),
            PredefinedPlanDefinition.ai("AI Credits 100", BillingCycle.YEARLY, "900.00", 100),
            PredefinedPlanDefinition.ai("AI Credits 200", BillingCycle.YEARLY, "1800.00", 200),
            PredefinedPlanDefinition.ai("AI Credits 500", BillingCycle.YEARLY, "4000.00", 500),
            PredefinedPlanDefinition.ai("AI Credits 1000", BillingCycle.YEARLY, "7000.00", 1000),
            PredefinedPlanDefinition.design("Designs 10", BillingCycle.MONTHLY, "15000.00", 10),
            PredefinedPlanDefinition.design("Designs 20", BillingCycle.MONTHLY, "26000.00", 20),
            PredefinedPlanDefinition.design("Designs 50", BillingCycle.MONTHLY, "50000.00", 50),
            PredefinedPlanDefinition.design("Designs 100", BillingCycle.MONTHLY, "80000.00", 100),
            PredefinedPlanDefinition.design("Designs 100", BillingCycle.YEARLY, "150000.00", 100),
            PredefinedPlanDefinition.design("Designs 200", BillingCycle.YEARLY, "260000.00", 200),
            PredefinedPlanDefinition.design("Designs 500", BillingCycle.YEARLY, "500000.00", 500),
            PredefinedPlanDefinition.design("Designs 1000", BillingCycle.YEARLY, "800000.00", 1000)
    );

    private final PlanRepository planRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;
        int updated = 0;

        for (PredefinedPlanDefinition definition : PREDEFINED_PLANS) {
            Plan plan = planRepository
                    .findByNameAndPlanTypeAndBillingCycle(
                            definition.name(),
                            definition.planType(),
                            definition.billingCycle()
                    )
                    .orElse(null);

            if (plan == null) {
                planRepository.save(buildPlan(definition));
                created++;
                continue;
            }

            if (applyDefinition(plan, definition)) {
                planRepository.save(plan);
                updated++;
            }
        }

        log.info("Predefined plan sync completed. Created: {}, updated: {}", created, updated);
    }

    private Plan buildPlan(PredefinedPlanDefinition definition) {
        return Plan.builder()
                .name(definition.name())
                .planType(definition.planType())
                .billingCycle(definition.billingCycle())
                .price(definition.price())
                .pricePerDesign(definition.pricePerDesign())
                .designLimit(definition.designLimit())
                .creditLimit(definition.creditLimit())
                .isActive(true)
                .build();
    }

    private boolean applyDefinition(Plan plan, PredefinedPlanDefinition definition) {
        boolean changed = false;

        if (plan.getPrice() == null || plan.getPrice().compareTo(definition.price()) != 0) {
            plan.setPrice(definition.price());
            changed = true;
        }
        if (!equalsNullable(plan.getPricePerDesign(), definition.pricePerDesign())) {
            plan.setPricePerDesign(definition.pricePerDesign());
            changed = true;
        }
        if (!definition.designLimit().equals(plan.getDesignLimit())) {
            plan.setDesignLimit(definition.designLimit());
            changed = true;
        }
        if (!definition.creditLimit().equals(plan.getCreditLimit())) {
            plan.setCreditLimit(definition.creditLimit());
            changed = true;
        }
        if (!Boolean.TRUE.equals(plan.getIsActive())) {
            plan.setIsActive(true);
            changed = true;
        }

        return changed;
    }

    private boolean equalsNullable(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private record PredefinedPlanDefinition(
            String name,
            PlanType planType,
            BillingCycle billingCycle,
            BigDecimal price,
            BigDecimal pricePerDesign,
            Integer designLimit,
            Integer creditLimit
    ) {
        private static PredefinedPlanDefinition ai(String name, BillingCycle billingCycle, String price, int credits) {
            return new PredefinedPlanDefinition(
                    name,
                    PlanType.AI,
                    billingCycle,
                    new BigDecimal(price),
                    null,
                    0,
                    credits
            );
        }

        private static PredefinedPlanDefinition design(String name, BillingCycle billingCycle, String price, int designs) {
            BigDecimal planPrice = new BigDecimal(price);
            return new PredefinedPlanDefinition(
                    name,
                    PlanType.DESIGN,
                    billingCycle,
                    planPrice,
                    planPrice.divide(BigDecimal.valueOf(designs), 2, RoundingMode.HALF_UP),
                    designs,
                    0
            );
        }
    }
}
