package com.rdc.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/bundles")
public class BundleController {

    // private final BundleService bundleService; // Inject service

    // POST /api/admin/bundles — create bundle
    @PostMapping
    public ResponseEntity<?> createBundle(@RequestBody Object request) {
        // return ResponseEntity.status(HttpStatus.CREATED).body(bundleService.createBundle(request));
        return ResponseEntity.status(HttpStatus.CREATED).body("Bundle created (placeholder)");
    }

    // GET /api/admin/bundles
    @GetMapping
    public List<?> listBundles() {
        // return bundleService.findAll();
        return List.of("Bundle A", "Bundle B");
    }

    // PUT /api/admin/bundles/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBundle(@PathVariable Long id, @RequestBody Object request) {
        // return ResponseEntity.ok(bundleService.updateBundle(id, request));
        return ResponseEntity.ok("Bundle updated (placeholder)");
    }

    // DELETE /api/admin/bundles/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBundle(@PathVariable Long id) {
        // bundleService.delete(id);
    }
}