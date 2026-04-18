package com.rdc.subscription.service;

import com.rdc.subscription.dto.internal.*;
import com.rdc.subscription.entity.CreditTransaction;
import com.rdc.subscription.entity.CreditWallet;
import com.rdc.subscription.entity.DesignUsage;
import com.rdc.subscription.entity.Subscription;
import com.rdc.subscription.enums.CreditTransactionType;
import com.rdc.subscription.enums.PlanType;
import com.rdc.subscription.enums.SubscriptionStatus;
import com.rdc.subscription.repository.CreditTransactionRepository;
import com.rdc.subscription.repository.CreditWalletRepository;
import com.rdc.subscription.repository.DesignUsageRepository;
import com.rdc.subscription.repository.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntitlementService {

    private final SubscriptionRepository subscriptionRepository;
    private final CreditWalletRepository creditWalletRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final DesignUsageRepository designUsageRepository;

    public AiValidationResponse validateAiUsage(AiValidationRequest request) {
        Subscription subscription = getActiveSubscription(request.getUserId());
        if (subscription == null) {
            return AiValidationResponse.builder()
                    .allowed(false)
                    .message("No active subscription")
                    .availableCredits(0)
                    .creditsRequired(request.getCreditsRequired())
                    .build();
        }

        PlanType planType = subscription.getPlan().getPlanType();
        if (!(planType == PlanType.AI || planType == PlanType.COMBO)) {
            return AiValidationResponse.builder()
                    .allowed(false)
                    .message("Current plan does not include AI access")
                    .availableCredits(0)
                    .creditsRequired(request.getCreditsRequired())
                    .build();
        }

        CreditWallet wallet = creditWalletRepository.findByUserId(request.getUserId()).orElse(null);
        int availableCredits = wallet != null ? wallet.getAvailableCredits() : 0;

        if (availableCredits < request.getCreditsRequired()) {
            return AiValidationResponse.builder()
                    .allowed(false)
                    .message("Insufficient credits")
                    .availableCredits(availableCredits)
                    .creditsRequired(request.getCreditsRequired())
                    .build();
        }

        return AiValidationResponse.builder()
                .allowed(true)
                .message("AI usage allowed")
                .availableCredits(availableCredits)
                .creditsRequired(request.getCreditsRequired())
                .build();
    }

    @Transactional
    public AiValidationResponse consumeAiUsage(ConsumeAiRequest request) {
        Subscription subscription = getActiveSubscription(request.getUserId());
        if (subscription == null) {
            throw new RuntimeException("No active subscription");
        }

        PlanType planType = subscription.getPlan().getPlanType();
        if (!(planType == PlanType.AI || planType == PlanType.COMBO)) {
            throw new RuntimeException("Current plan does not include AI access");
        }

        CreditWallet wallet = creditWalletRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Credit wallet not found"));

        if (wallet.getAvailableCredits() < request.getCreditsRequired()) {
            throw new RuntimeException("Insufficient credits");
        }

        wallet.setAvailableCredits(wallet.getAvailableCredits() - request.getCreditsRequired());
        creditWalletRepository.save(wallet);

        CreditTransaction txn = CreditTransaction.builder()
                .userId(request.getUserId())
                .subscription(subscription)
                .type(CreditTransactionType.DEDUCT)
                .amount(request.getCreditsRequired())
                .toolName(request.getToolName())
                .description("Credits deducted for AI tool usage")
                .build();

        creditTransactionRepository.save(txn);

        return AiValidationResponse.builder()
                .allowed(true)
                .message("AI credits consumed successfully")
                .availableCredits(wallet.getAvailableCredits())
                .creditsRequired(request.getCreditsRequired())
                .build();
    }

    public DesignValidationResponse validateDesignUsage(DesignValidationRequest request) {
        Subscription subscription = getActiveSubscription(request.getUserId());
        if (subscription == null) {
            return DesignValidationResponse.builder()
                    .allowed(false)
                    .message("No active subscription")
                    .designLimit(0)
                    .usedDesigns(0)
                    .remainingDesigns(0)
                    .build();
        }

        PlanType planType = subscription.getPlan().getPlanType();
        if (!(planType == PlanType.DESIGN || planType == PlanType.COMBO)) {
            return DesignValidationResponse.builder()
                    .allowed(false)
                    .message("Current plan does not include design access")
                    .designLimit(0)
                    .usedDesigns(0)
                    .remainingDesigns(0)
                    .build();
        }

        DesignUsage usage = designUsageRepository
                .findFirstByUserIdAndSubscriptionIdOrderByUpdatedAtDesc(request.getUserId(), subscription.getId())
                .orElse(null);

        int used = usage != null ? usage.getUsedCount() : 0;
        int limit = subscription.getPlan().getDesignLimit();
        int remaining = Math.max(limit - used, 0);

        if (used >= limit) {
            return DesignValidationResponse.builder()
                    .allowed(false)
                    .message("Design limit exhausted")
                    .designLimit(limit)
                    .usedDesigns(used)
                    .remainingDesigns(remaining)
                    .build();
        }

        return DesignValidationResponse.builder()
                .allowed(true)
                .message("Design usage allowed")
                .designLimit(limit)
                .usedDesigns(used)
                .remainingDesigns(remaining)
                .build();
    }

    @Transactional
    public DesignValidationResponse consumeDesignUsage(ConsumeDesignRequest request) {
        Subscription subscription = getActiveSubscription(request.getUserId());
        if (subscription == null) {
            throw new RuntimeException("No active subscription");
        }

        PlanType planType = subscription.getPlan().getPlanType();
        if (!(planType == PlanType.DESIGN || planType == PlanType.COMBO)) {
            throw new RuntimeException("Current plan does not include design access");
        }

        DesignUsage usage = designUsageRepository
                .findFirstByUserIdAndSubscriptionIdOrderByUpdatedAtDesc(request.getUserId(), subscription.getId())
                .orElseThrow(() -> new RuntimeException("Design usage record not found"));

        int limit = subscription.getPlan().getDesignLimit();
        if (usage.getUsedCount() >= limit) {
            throw new RuntimeException("Design limit exhausted");
        }

        usage.setUsedCount(usage.getUsedCount() + 1);
        designUsageRepository.save(usage);

        int remaining = Math.max(limit - usage.getUsedCount(), 0);

        return DesignValidationResponse.builder()
                .allowed(true)
                .message("Design usage consumed successfully")
                .designLimit(limit)
                .usedDesigns(usage.getUsedCount())
                .remainingDesigns(remaining)
                .build();
    }

    private Subscription getActiveSubscription(Long userId) {
        return subscriptionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .orElse(null);
    }
}