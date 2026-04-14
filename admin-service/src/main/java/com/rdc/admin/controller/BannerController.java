package com.rdc.admin.controller;

import com.rdc.admin.dto.BannerRequest;
import com.rdc.admin.dto.BannerResponse;
import com.rdc.admin.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    // --- PUBLIC ENDPOINT ---
    @GetMapping("/api/banners/active")
    public ResponseEntity<BannerResponse> getActiveBanner() {
        return ResponseEntity.ok(bannerService.getActiveBanner());
    }

    // --- ADMIN CMS ENDPOINTS (CRUD) ---
    @PostMapping("/api/admin/banners")
    public ResponseEntity<BannerResponse> createBanner(@RequestBody BannerRequest request) {
        return new ResponseEntity<>(bannerService.createBanner(request), HttpStatus.CREATED);
    }

    @GetMapping("/api/admin/banners")
    public ResponseEntity<List<BannerResponse>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAllBanners());
    }

    @PutMapping("/api/admin/banners/{id}")
    public ResponseEntity<BannerResponse> updateBanner(
            @PathVariable Long id,
            @RequestBody BannerRequest request) {
        return ResponseEntity.ok(bannerService.updateBanner(id, request));
    }

    @DeleteMapping("/api/admin/banners/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        bannerService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }
}