package com.rdc.subscription.service;

import com.rdc.subscription.dto.PurchaseSubscriptionRequest;
import com.rdc.subscription.dto.PurchaseSubscriptionResponse;
import com.rdc.subscription.entity.CreditTransaction;
import com.rdc.subscription.entity.CreditWallet;
import com.rdc.subscription.entity.DesignUsage;
import com.rdc.subscription.entity.Plan;
import com.rdc.subscription.entity.Subscription;
import com.rdc.subscription.enums.BillingCycle;
import com.rdc.subscription.enums.CreditTransactionType;
import com.rdc.subscription.enums.SubscriptionStatus;
import com.rdc.subscription.repository.CreditTransactionRepository;
import com.rdc.subscription.repository.CreditWalletRepository;
import com.rdc.subscription.repository.DesignUsageRepository;
import com.rdc.subscription.repository.PlanRepository;
import com.rdc.subscription.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionCommandService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CreditWalletRepository creditWalletRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final DesignUsageRepository designUsageRepository;

    @Transactional
    public PurchaseSubscriptionResponse createSubscriptionForUser(Long userId, PurchaseSubscriptionRequest request) {
        Plan plan = planRepository.findByIdAndIsActiveTrue(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Active plan not found"));

        subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new RuntimeException("User already has an active subscription");
                });

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = calculateEndDate(start, plan.getBillingCycle());

        Subscription subscription = Subscription.builder()
                .userId(userId)
                .plan(plan)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(start)
                .endDate(end)
                .autoRenew(false)
                .build();

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        CreditWallet wallet = creditWalletRepository.findByUserId(userId)
                .orElse(
                        CreditWallet.builder()
                                .userId(userId)
                                .availableCredits(0)
                                .build()
                );

        wallet.setAvailableCredits(plan.getCreditLimit());
        creditWalletRepository.save(wallet);

        if (plan.getCreditLimit() > 0) {
            CreditTransaction txn = CreditTransaction.builder()
                    .userId(userId)
                    .subscription(savedSubscription)
                    .type(CreditTransactionType.ADD)
                    .amount(plan.getCreditLimit())
                    .toolName(null)
                    .description("Credits added on subscription activation")
                    .build();

            creditTransactionRepository.save(txn);
        }

        DesignUsage usage = DesignUsage.builder()
                .userId(userId)
                .subscription(savedSubscription)
                .totalAllowed(plan.getDesignLimit())
                .usedCount(0)
                .remainingCount(plan.getDesignLimit())
                .periodStart(start)
                .periodEnd(end)
                .build();

        designUsageRepository.save(usage);

        return PurchaseSubscriptionResponse.builder()
                .planId(plan.getId())
                .planName(plan.getName())
                .message("Subscription activated successfully")
                .build();
    }

    private LocalDateTime calculateEndDate(LocalDateTime start, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> start.plusMonths(1);
            case YEARLY -> start.plusYears(1);
        };
    }
}
