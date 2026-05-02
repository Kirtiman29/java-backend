package com.rdc.admin.controller;

import com.rdc.admin.dto.DesignDownloadRequestResponse;
import com.rdc.admin.service.DesignDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/design-download-requests")
public class AdminDesignDownloadController {

    private final DesignDownloadService designDownloadService;

    @GetMapping
    public ResponseEntity<List<DesignDownloadRequestResponse>> getAllRequests() {
        return ResponseEntity.ok(designDownloadService.getAllRequests());
    }

    @PatchMapping("/{requestId}/sent")
    public ResponseEntity<DesignDownloadRequestResponse> markAsSent(@PathVariable Long requestId) {
        return ResponseEntity.ok(designDownloadService.markAsSent(requestId));
    }
}
