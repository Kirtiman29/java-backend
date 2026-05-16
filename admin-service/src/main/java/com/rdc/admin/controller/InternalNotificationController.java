package com.rdc.admin.controller;

import com.rdc.admin.dto.notification.CreateNotificationRequest;
import com.rdc.admin.dto.notification.NotificationResponse;
import com.rdc.admin.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/notifications")
@Slf4j
public class InternalNotificationController {

    private final NotificationService notificationService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key
    ) {
        validateKey(key);
        return new ResponseEntity<>(
                notificationService.createUserNotification(request),
                HttpStatus.CREATED
        );
    }

    private void validateKey(String key) {
        if (internalServiceKey == null || !internalServiceKey.equals(key)) {
            log.error("Access denied for internal notification request");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service key");
        }
    }
}
