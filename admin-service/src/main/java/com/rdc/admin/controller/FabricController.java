package com.rdc.admin.controller;

import com.rdc.admin.dto.*;
import com.rdc.admin.service.FabricService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FabricController {

    private final FabricService fabricService;

    @PostMapping("/api/admin/fabrics")
    public ResponseEntity<FabricResponse> create(@RequestBody FabricCreateRequest req) {
        return new ResponseEntity<>(fabricService.create(req), HttpStatus.CREATED);
    }

    @PutMapping("/api/admin/fabrics/{id}")
    public ResponseEntity<FabricResponse> update(@PathVariable Long id, @RequestBody FabricUpdateRequest req) {
        return ResponseEntity.ok(fabricService.update(id, req));
    }

    @GetMapping("/api/admin/fabrics")
    public ResponseEntity<List<FabricResponse>> getAllAdmin() {
        return ResponseEntity.ok(fabricService.getAllAdmin());
    }

    @DeleteMapping("/api/admin/fabrics/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fabricService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/fabrics/bulk")
    public ResponseEntity<BulkUploadResponse> bulkUpload(
            @RequestParam("csv") MultipartFile csv,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "attachment", required = false) MultipartFile[] attachments) {
        try {
            return ResponseEntity.ok(fabricService.processBulk(csv.getInputStream(), mergeFiles(files, attachments)));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/public/fabrics")
    public ResponseEntity<List<FabricResponse>> getAll() {
        return ResponseEntity.ok(fabricService.getAll());
    }

    @GetMapping("/api/public/fabrics/{id}")
    public ResponseEntity<FabricResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(fabricService.getById(id));
    }

    private MultipartFile[] mergeFiles(MultipartFile[] files, MultipartFile[] attachments) {
        if (files == null || files.length == 0) {
            return attachments;
        }
        if (attachments == null || attachments.length == 0) {
            return files;
        }

        MultipartFile[] merged = new MultipartFile[files.length + attachments.length];
        System.arraycopy(files, 0, merged, 0, files.length);
        System.arraycopy(attachments, 0, merged, files.length, attachments.length);
        return merged;
    }
}
