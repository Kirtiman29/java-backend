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
@RequestMapping("/api/admin/designs")
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;

    @PostMapping
    public ResponseEntity<DesignResponse> createDesign(@RequestBody DesignCreateRequest request) {
        return new ResponseEntity<>(designService.createDesign(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DesignResponse>> getAllDesigns() {
        // FIX: Changed from findAll() to getAllDesigns()
        return ResponseEntity.ok(designService.getAllDesigns());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getDesignById(@PathVariable Long id) {
        // FIX: Changed from findById() to getDesignById()
        return ResponseEntity.ok(designService.getDesignById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignResponse> updateDesign(
            @PathVariable Long id,
            @RequestBody DesignUpdateRequest request) {
        return ResponseEntity.ok(designService.updateDesign(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        // FIX: Changed from deleteById() to deleteDesign()
        designService.deleteDesign(id);
        return ResponseEntity.noContent().build();
    }
}