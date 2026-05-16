package com.rdc.admin.repository;

import com.rdc.admin.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByReferenceKey(String referenceKey);

    @Query("""
            select n
            from Notification n
            where (n.userId = :userId or n.globalNotification = true)
              and (n.expiresAt is null or n.expiresAt >= :now)
            order by n.createdAt desc
            """)
    List<Notification> findVisibleNotifications(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Query("""
            select n
            from Notification n
            where n.globalNotification = true
              and (n.expiresAt is null or n.expiresAt >= :now)
            order by n.createdAt desc
            """)
    List<Notification> findActiveGlobalNotifications(@Param("now") LocalDateTime now);

    @Query("""
            select n
            from Notification n
            where n.userId = :userId
              and n.readStatus = false
              and (n.expiresAt is null or n.expiresAt >= :now)
            order by n.createdAt desc
            """)
    List<Notification> findUnreadUserNotifications(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    @Query("""
            select count(n)
            from Notification n
            where n.userId = :userId
              and n.readStatus = false
              and (n.expiresAt is null or n.expiresAt >= :now)
            """)
    long countUnreadUserNotifications(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );
}
