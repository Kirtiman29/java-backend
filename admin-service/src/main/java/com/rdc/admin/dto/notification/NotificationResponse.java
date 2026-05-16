package com.rdc.admin.dto.notification;

import com.rdc.admin.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private String targetUrl;
    private boolean read;
    private boolean global;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
