package com.rdc.subscription.service;

import com.rdc.subscription.dto.SubscriptionSummaryResponse;
import com.rdc.subscription.entity.CreditWallet;
import com.rdc.subscription.entity.DesignUsage;
import com.rdc.subscription.entity.Subscription;
import com.rdc.subscription.enums.SubscriptionStatus;
import com.rdc.subscription.repository.CreditWalletRepository;
import com.rdc.subscription.repository.DesignUsageRepository;
import com.rdc.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionQueryService {

    private final SubscriptionRepository subscriptionRepository;
    private final CreditWalletRepository creditWalletRepository;
    private final DesignUsageRepository designUsageRepository;

    public SubscriptionSummaryResponse getMySubscription(Long userId) {
        return getSubscriptionSummary(userId);
    }

    public SubscriptionSummaryResponse getSubscriptionSummary(Long userId) {
        Subscription subscription = subscriptionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .orElse(null);

        if (subscription == null) {
            return SubscriptionSummaryResponse.builder()
                    .subscriptionId(null)
                    .status(null)
                    .planId(null)
                    .planName(null)
                    .planType(null)
                    .billingCycle(null)
                    .designLimit(0)
                    .creditLimit(0)
                    .availableCredits(0)
                    .usedDesigns(0)
                    .remainingDesigns(0)
                    .pricePerDesign(null)
                    .build();
        }

        CreditWallet wallet = creditWalletRepository.findByUserId(userId).orElse(null);
        DesignUsage designUsage = designUsageRepository
                .findFirstByUserIdAndSubscriptionIdOrderByUpdatedAtDesc(userId, subscription.getId())
                .orElse(null);

        return SubscriptionSummaryResponse.builder()
                .subscriptionId(subscription.getId())
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .planId(subscription.getPlan().getId())
                .planName(subscription.getPlan().getName())
                .planType(subscription.getPlan().getPlanType())
                .billingCycle(subscription.getPlan().getBillingCycle())
                .designLimit(subscription.getPlan().getDesignLimit())
                .creditLimit(subscription.getPlan().getCreditLimit())
                .availableCredits(wallet != null ? wallet.getAvailableCredits() : 0)
                .usedDesigns(designUsage != null ? designUsage.getUsedCount() : 0)
                .remainingDesigns(designUsage != null
                        ? Math.max(subscription.getPlan().getDesignLimit() - designUsage.getUsedCount(), 0)
                        : subscription.getPlan().getDesignLimit())
                .pricePerDesign(subscription.getPlan().getPricePerDesign())
                .build();
    }
}
