package com.rdc.admin.controller;

import com.rdc.admin.dto.DesignCreateRequest;
import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.dto.DesignUpdateRequest;
import com.rdc.admin.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/designs") // 🔒 Strictly for ROLE_ADMIN
public class DesignController {

    private final DesignService designService;

    /**
     * Creates a new design.
     * The tags in DesignCreateRequest will be persisted via @ElementCollection.
     */
    @PostMapping
    public ResponseEntity<DesignResponse> createDesign(@RequestBody DesignCreateRequest request) {
        return new ResponseEntity<>(designService.createDesign(request), HttpStatus.CREATED);
    }

    /**
     * Fetches all designs for the admin dashboard.
     */
    @GetMapping
    public ResponseEntity<List<DesignResponse>> getAllDesignsAdmin() {
        // Ensure this matches the method name in DesignService.java
        return ResponseEntity.ok(designService.getAllDesigns());
    }

    /**
     * Updates an existing design.
     * Ensure DesignUpdateRequest.getTags() is not null to update design_tags table.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DesignResponse> updateDesign(
            @PathVariable Long id,
            @RequestBody DesignUpdateRequest request) {
        return ResponseEntity.ok(designService.updateDesign(id, request));
    }

    /**
     * Deletes a design and its associated tags (handled automatically by JPA).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        designService.deleteDesign(id);
        return ResponseEntity.noContent().build();
    }
}