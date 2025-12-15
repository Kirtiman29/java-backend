package com.rdc.admin.controller;

import com.rdc.admin.dto.DesignCreateRequest;
import com.rdc.admin.dto.DesignUpdateRequest;
import com.rdc.admin.dto.DesignResponse; // Updated to use DTO
import com.rdc.admin.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/designs")
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;

    @PostMapping
    @Transactional
    public ResponseEntity<DesignResponse> createDesign(@Valid @RequestBody DesignCreateRequest request) {
        DesignResponse newDesign = designService.createDesign(request);
        return new ResponseEntity<>(newDesign, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<DesignResponse>> getAllDesigns() {
        List<DesignResponse> designs = designService.getAllDesigns();
        return ResponseEntity.ok(designs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getDesignById(@PathVariable Long id) {
        DesignResponse design = designService.getDesignById(id);
        return ResponseEntity.ok(design);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignResponse> updateDesign(@PathVariable Long id, @Valid @RequestBody DesignUpdateRequest request) {
        DesignResponse updatedDesign = designService.updateDesign(id, request);
        return ResponseEntity.ok(updatedDesign);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDesign(@PathVariable Long id) {
        designService.deleteDesign(id);
        return ResponseEntity.noContent().build();
    }
}