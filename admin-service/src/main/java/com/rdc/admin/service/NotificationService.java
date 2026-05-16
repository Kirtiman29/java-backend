package com.rdc.admin.service;

import com.rdc.admin.dto.notification.CreateGlobalNotificationRequest;
import com.rdc.admin.dto.notification.CreateNotificationRequest;
import com.rdc.admin.dto.notification.NotificationCountResponse;
import com.rdc.admin.dto.notification.NotificationResponse;
import com.rdc.admin.entity.Notification;
import com.rdc.admin.entity.NotificationRead;
import com.rdc.admin.repository.NotificationReadRepository;
import com.rdc.admin.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> notifications = notificationRepository.findVisibleNotifications(userId, now);

        List<Long> globalIds = notifications.stream()
                .filter(notification -> Boolean.TRUE.equals(notification.getGlobalNotification()))
                .map(Notification::getId)
                .toList();

        Set<Long> readGlobalIds = globalIds.isEmpty()
                ? Set.of()
                : new HashSet<>(notificationReadRepository.findReadNotificationIds(userId, globalIds));

        return notifications.stream()
                .map(notification -> mapResponse(notification, isRead(notification, readGlobalIds)))
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationCountResponse getUnreadCount(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        long unreadUserCount = notificationRepository.countUnreadUserNotifications(userId, now);
        long unreadGlobalCount = notificationReadRepository.countUnreadGlobalNotifications(userId, now);
        return new NotificationCountResponse(unreadUserCount + unreadGlobalCount);
    }

    @Transactional
    public NotificationResponse createUserNotification(CreateNotificationRequest request) {
        Notification notification = findExistingReference(request.getReferenceKey())
                .orElseGet(() -> notificationRepository.save(
                        Notification.builder()
                                .userId(request.getUserId())
                                .title(request.getTitle())
                                .message(request.getMessage())
                                .type(request.getType())
                                .targetUrl(request.getTargetUrl())
                                .globalNotification(false)
                                .readStatus(false)
                                .referenceKey(cleanReferenceKey(request.getReferenceKey()))
                                .expiresAt(request.getExpiresAt())
                                .build()
                ));

        return mapResponse(notification, Boolean.TRUE.equals(notification.getReadStatus()));
    }

    @Transactional
    public NotificationResponse createGlobalNotification(CreateGlobalNotificationRequest request) {
        Notification notification = findExistingReference(request.getReferenceKey())
                .orElseGet(() -> notificationRepository.save(
                        Notification.builder()
                                .title(request.getTitle())
                                .message(request.getMessage())
                                .type(request.getType())
                                .targetUrl(request.getTargetUrl())
                                .globalNotification(true)
                                .readStatus(false)
                                .referenceKey(cleanReferenceKey(request.getReferenceKey()))
                                .expiresAt(request.getExpiresAt())
                                .build()
                ));

        return mapResponse(notification, false);
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));

        if (Boolean.TRUE.equals(notification.getGlobalNotification())) {
            markGlobalNotificationAsRead(userId, notification);
            return;
        }

        if (!userId.equals(notification.getUserId())) {
            throw new ResponseStatusException(NOT_FOUND, "Notification not found");
        }

        if (!Boolean.TRUE.equals(notification.getReadStatus())) {
            notification.setReadStatus(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        List<Notification> unreadUserNotifications = notificationRepository.findUnreadUserNotifications(userId, now);
        if (!unreadUserNotifications.isEmpty()) {
            unreadUserNotifications.forEach(notification -> {
                notification.setReadStatus(true);
                notification.setReadAt(now);
            });
            notificationRepository.saveAll(unreadUserNotifications);
        }

        List<Notification> activeGlobalNotifications = notificationRepository.findActiveGlobalNotifications(now);
        if (activeGlobalNotifications.isEmpty()) {
            return;
        }

        List<Long> globalIds = activeGlobalNotifications.stream()
                .map(Notification::getId)
                .toList();

        Set<Long> readGlobalIds = new HashSet<>(notificationReadRepository.findReadNotificationIds(userId, globalIds));

        List<NotificationRead> readsToCreate = activeGlobalNotifications.stream()
                .filter(notification -> !readGlobalIds.contains(notification.getId()))
                .map(notification -> NotificationRead.builder()
                        .notification(notification)
                        .userId(userId)
                        .readAt(now)
                        .build())
                .toList();

        if (!readsToCreate.isEmpty()) {
            notificationReadRepository.saveAll(readsToCreate);
        }
    }

    private void markGlobalNotificationAsRead(Long userId, Notification notification) {
        boolean alreadyRead = notificationReadRepository.existsByNotification_IdAndUserId(notification.getId(), userId);
        if (alreadyRead) {
            return;
        }

        notificationReadRepository.save(
                NotificationRead.builder()
                        .notification(notification)
                        .userId(userId)
                        .readAt(LocalDateTime.now())
                        .build()
        );
    }

    private java.util.Optional<Notification> findExistingReference(String referenceKey) {
        String cleanedReferenceKey = cleanReferenceKey(referenceKey);
        if (cleanedReferenceKey == null) {
            return java.util.Optional.empty();
        }
        return notificationRepository.findByReferenceKey(cleanedReferenceKey);
    }

    private String cleanReferenceKey(String referenceKey) {
        if (referenceKey == null || referenceKey.isBlank()) {
            return null;
        }
        return referenceKey.trim();
    }

    private boolean isRead(Notification notification, Set<Long> readGlobalIds) {
        if (Boolean.TRUE.equals(notification.getGlobalNotification())) {
            return readGlobalIds.contains(notification.getId());
        }
        return Boolean.TRUE.equals(notification.getReadStatus());
    }

    private NotificationResponse mapResponse(Notification notification, boolean read) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .targetUrl(notification.getTargetUrl())
                .read(read)
                .global(Boolean.TRUE.equals(notification.getGlobalNotification()))
                .createdAt(notification.getCreatedAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }
}
