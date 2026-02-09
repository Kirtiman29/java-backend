package com.rdc.admin.controller;

import com.rdc.admin.dto.DesignCreateRequest;
import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.dto.DesignUpdateRequest;
import com.rdc.admin.entity.DesignDeletionRecord;
import com.rdc.admin.repository.DesignDeletionRecordRepository;
import com.rdc.admin.service.DesignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/designs") // 🔒 Strictly for ROLE_ADMIN via SecurityConfig
@Slf4j
public class DesignController {

    private final DesignService designService;
    private final DesignDeletionRecordRepository deletionRecordRepository;

    /**
     * ✅ NEW: Fetch Audit Logs for React Frontend
     * Path: GET /api/admin/designs/purge/records
     */
    @GetMapping("/purge/records")
    public ResponseEntity<List<DesignDeletionRecord>> getPurgeRecords() {
        log.info("📋 Admin Request: Fetching design purge audit logs");
        // Using the custom descending sort we added to the repository
        return ResponseEntity.ok(deletionRecordRepository.findAllByOrderByDeletedAtDesc());
    }

    /**
     * Fetches all designs for the admin dashboard.
     */
    @GetMapping
    public ResponseEntity<List<DesignResponse>> getAllDesignsAdmin() {
        return ResponseEntity.ok(designService.getAllDesigns());
    }

    /**
     * Creates a new design.
     */
    @PostMapping
    public ResponseEntity<DesignResponse> createDesign(@RequestBody DesignCreateRequest request) {
        log.info("🎨 Admin Request: Creating new design with SKU: {}", request.getDesignIdentifier());
        return new ResponseEntity<>(designService.createDesign(request), HttpStatus.CREATED);
    }

    /**
     * Updates an existing design.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DesignResponse> updateDesign(
            @PathVariable Long id,
            @RequestBody DesignUpdateRequest request) {
        log.info("✏️ Admin Request: Updating design ID: {}", id);
        return ResponseEntity.ok(designService.updateDesign(id, request));
    }

    /**
     * ✅ NEW: Mark as Sold (Called by Order Service)
     * Path: POST /api/admin/designs/{id}/sold
     */
    @PostMapping("/{id}/sold")
    public ResponseEntity<Void> markAsSold(@PathVariable Long id) {
        designService.markDesignAsSold(id);
        return ResponseEntity.ok().build();
    }

    /**
     * ✅ NEW: Purge Design (Called by Order Service post-payment)
     * Path: POST /api/admin/designs/{id}/purge
     */
    @PostMapping("/{id}/purge")
    public ResponseEntity<Void> purgeDesign(@PathVariable Long id, @RequestParam Long orderId) {
        log.info("🔥 Internal Request: Purging design {} for Order {}", id, orderId);
        designService.purgeDesignAndRecord(id, orderId);
        return ResponseEntity.ok().build();
    }

    /**
     * Manual hard delete from Admin Panel.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        log.warn("🗑️ Admin Request: Manual permanent deletion for design ID: {}", id);
        designService.deleteDesign(id);
        return ResponseEntity.noContent().build();
    }
}