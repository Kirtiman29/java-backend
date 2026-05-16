package com.rdc.order.dto.internal;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalNotificationRequest {

    private Long userId;
    private String title;
    private String message;
    private String type;
    private String targetUrl;
    private String referenceKey;
    private LocalDateTime expiresAt;
}
