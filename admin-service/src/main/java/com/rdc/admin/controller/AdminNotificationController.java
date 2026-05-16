package com.rdc.admin.controller;

import com.rdc.admin.dto.notification.CreateGlobalNotificationRequest;
import com.rdc.admin.dto.notification.NotificationResponse;
import com.rdc.admin.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @PostMapping("/global")
    public ResponseEntity<NotificationResponse> createGlobalNotification(
            @Valid @RequestBody CreateGlobalNotificationRequest request
    ) {
        return new ResponseEntity<>(
                notificationService.createGlobalNotification(request),
                HttpStatus.CREATED
        );
    }
}
