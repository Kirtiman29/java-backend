package com.rdc.admin.controller;

import com.rdc.admin.dto.BulkUploadResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/designs")
@Slf4j
public class DesignController {

    private final DesignService designService;
    private final DesignDeletionRecordRepository deletionRecordRepository;
    @GetMapping("/purge/records")
    public ResponseEntity<List<DesignDeletionRecord>> getPurgeRecords() {
        log.info("Admin Request: Fetching design purge audit logs");
        // Using the custom descending sort we added to the repository
        return ResponseEntity.ok(deletionRecordRepository.findAllByOrderByDeletedAtDesc());
    }
    @GetMapping
    public ResponseEntity<List<DesignResponse>> getAllDesignsAdmin() {
        return ResponseEntity.ok(designService.getAllDesigns());
    }
    @PostMapping
    public ResponseEntity<DesignResponse> createDesign(@RequestBody DesignCreateRequest request) {
        log.info("Admin Request: Creating new design with SKU: {}", request.getDesignIdentifier());
        return new ResponseEntity<>(designService.createDesign(request), HttpStatus.CREATED);
    }
    @PutMapping("/{id}")
    public ResponseEntity<DesignResponse> updateDesign(
            @PathVariable Long id,
            @RequestBody DesignUpdateRequest request) {
        log.info("Admin Request: Updating design ID: {}", id);
        return ResponseEntity.ok(designService.updateDesign(id, request));
    }
    @PostMapping("/{id}/sold")
    public ResponseEntity<Void> markAsSold(@PathVariable Long id) {
        designService.markDesignAsSold(id);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/{id}/purge")
    public ResponseEntity<Void> purgeDesign(@PathVariable Long id, @RequestParam Long orderId) {
        log.info("Internal Request: Purging design {} for Order {}", id, orderId);
        designService.purgeDesignAndRecord(id, orderId);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        log.warn("Admin Request: Manual permanent deletion for design ID: {}", id);
        designService.deleteDesign(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkUploadResponse> bulkUpload(
            @RequestParam("csv") MultipartFile csv,
            @RequestParam(value = "files", required = false) MultipartFile[] assets) {

        try {
            return ResponseEntity.ok(designService.processBulk(csv.getInputStream(), assets));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(500).build();
        }
    }

}
