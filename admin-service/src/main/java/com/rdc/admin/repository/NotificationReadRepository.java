package com.rdc.admin.repository;

import com.rdc.admin.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    boolean existsByNotification_IdAndUserId(Long notificationId, Long userId);

    @Query("""
            select nr.notification.id
            from NotificationRead nr
            where nr.userId = :userId
              and nr.notification.id in :notificationIds
            """)
    List<Long> findReadNotificationIds(
            @Param("userId") Long userId,
            @Param("notificationIds") List<Long> notificationIds
    );

    @Query("""
            select count(n)
            from Notification n
            where n.globalNotification = true
              and (n.expiresAt is null or n.expiresAt >= :now)
              and not exists (
                  select nr.id
                  from NotificationRead nr
                  where nr.notification = n
                    and nr.userId = :userId
              )
            """)
    long countUnreadGlobalNotifications(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
}
