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

    @PostMapping
    public ResponseEntity<DesignResponse> createDesign(@RequestBody DesignCreateRequest request) {
        // Implementation logic handles the initial COVER media role [cite: 147]
        return new ResponseEntity<>(designService.createDesign(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DesignResponse>> getAllDesignsAdmin() {
        return ResponseEntity.ok(designService.getAllDesigns());
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignResponse> updateDesign(
            @PathVariable Long id,
            @RequestBody DesignUpdateRequest request) {
        return ResponseEntity.ok(designService.updateDesign(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        designService.deleteDesign(id);
        return ResponseEntity.noContent().build();
    }
}