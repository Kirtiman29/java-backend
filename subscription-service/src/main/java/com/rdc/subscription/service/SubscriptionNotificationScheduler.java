package com.rdc.subscription.service;

import com.rdc.subscription.dto.internal.InternalNotificationRequest;
import com.rdc.subscription.entity.Subscription;
import com.rdc.subscription.enums.SubscriptionStatus;
import com.rdc.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionNotificationScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final NotificationServiceClient notificationServiceClient;

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void processSubscriptionNotifications() {
        notifyExpiringSubscriptions();
        expirePastDueSubscriptions();
    }

    private void notifyExpiringSubscriptions() {
        LocalDate targetDate = LocalDate.now().plusDays(3);
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.plusDays(1).atStartOfDay();

        List<Subscription> expiringSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBetween(SubscriptionStatus.ACTIVE, start, end);

        for (Subscription subscription : expiringSubscriptions) {
            notificationServiceClient.createUserNotification(
                    InternalNotificationRequest.builder()
                            .userId(subscription.getUserId())
                            .title("Subscription Expiring Soon")
                            .message("Your " + subscription.getPlan().getName() + " plan will expire in 3 days.")
                            .type("SUBSCRIPTION_EXPIRING")
                            .targetUrl("/subscriptions")
                            .referenceKey("subscription-expiring-" + subscription.getId() + "-3days")
                            .expiresAt(subscription.getEndDate())
                            .build()
            );
        }
    }

    private void expirePastDueSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<Subscription> expiredSubscriptions = subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, now);

        for (Subscription subscription : expiredSubscriptions) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);

            notificationServiceClient.createUserNotification(
                    InternalNotificationRequest.builder()
                            .userId(subscription.getUserId())
                            .title("Subscription Expired")
                            .message("Your " + subscription.getPlan().getName() + " plan has expired. Renew to continue using benefits.")
                            .type("SUBSCRIPTION_EXPIRED")
                            .targetUrl("/subscriptions")
                            .referenceKey("subscription-expired-" + subscription.getId())
                            .build()
            );
        }

        if (!expiredSubscriptions.isEmpty()) {
            subscriptionRepository.saveAll(expiredSubscriptions);
            log.info("Marked {} subscriptions as expired", expiredSubscriptions.size());
        }
    }
}
