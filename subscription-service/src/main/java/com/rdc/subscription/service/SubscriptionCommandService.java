package com.rdc.subscription.service;

import com.rdc.subscription.dto.PurchaseSubscriptionRequest;
import com.rdc.subscription.dto.PurchaseSubscriptionResponse;
import com.rdc.subscription.dto.internal.InternalNotificationRequest;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionCommandService {

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CreditWalletRepository creditWalletRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final DesignUsageRepository designUsageRepository;
    private final AuthServiceClient authServiceClient;
    private final SubscriptionInvoiceEmailService subscriptionInvoiceEmailService;
    private final NotificationServiceClient notificationServiceClient;

    @Transactional
    public PurchaseSubscriptionResponse createSubscriptionForUser(Long userId, PurchaseSubscriptionRequest request) {
        Plan plan = planRepository.findByIdAndIsActiveTrue(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Active plan not found"));

        subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .filter(existing -> existing.getEndDate().isAfter(LocalDateTime.now()))
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

        notificationServiceClient.createUserNotification(
                InternalNotificationRequest.builder()
                        .userId(userId)
                        .title("Subscription Activated")
                        .message("Your " + plan.getName() + " plan is now active.")
                        .type("SUBSCRIPTION_ACTIVATED")
                        .targetUrl("/subscriptions")
                        .referenceKey("subscription-activated-" + savedSubscription.getId())
                        .expiresAt(end)
                        .build()
        );

        sendSubscriptionInvoiceIfPossible(userId, plan, start);

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

    private void sendSubscriptionInvoiceIfPossible(Long userId, Plan plan, LocalDateTime activatedAt) {
        try {
            Map<String, Object> userMeta = authServiceClient.getUserMetadata(userId);
            if (userMeta == null) {
                return;
            }

            String email = (String) userMeta.get("email");
            if (email == null || email.isBlank()) {
                return;
            }

            String name = (String) userMeta.getOrDefault("name", "Customer");
            subscriptionInvoiceEmailService.sendSubscriptionInvoice(email, name, plan, activatedAt);
        } catch (Exception ex) {
            log.error("Failed to trigger subscription invoice email for user {} and plan {}: {}", userId, plan.getId(), ex.getMessage(), ex);
        }
    }
}
