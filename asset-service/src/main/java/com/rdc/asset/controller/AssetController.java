package com.rdc.asset.controller;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.entity.Asset;
import com.rdc.asset.model.AssetType;
import com.rdc.asset.repo.AssetRepository;
import com.rdc.asset.service.AssetService;
import com.rdc.asset.service.StorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetRepository assetRepo;
    private final StorageProvider storageProvider;

    @GetMapping
    public ResponseEntity<List<AssetDto>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    @PostMapping("/upload")
    public ResponseEntity<AssetDto> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("sellerId") Long sellerId,
            @RequestParam("type") AssetType type) throws Exception {

        AssetDto result = assetService.uploadAndCreateAsset(file, title, sellerId, type);
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    /**
     * ✅ DOWNLOAD ASSET (Public)
     * Matches SecurityConfig: .requestMatchers(HttpMethod.GET, "/api/assets/download/**").permitAll()
     */
    @GetMapping("/download/{uuid}")
    public ResponseEntity<InputStreamResource> downloadAsset(@PathVariable String uuid) throws Exception {

        // 🚨 Validation: Prevent "null" strings or empty paths from triggering internal errors
        if (uuid == null || uuid.trim().isEmpty() || uuid.equalsIgnoreCase("null")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Asset UUID provided");
        }

        Asset asset = assetRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .body(new InputStreamResource(storageProvider.read(asset.getFilename())));
    }
}