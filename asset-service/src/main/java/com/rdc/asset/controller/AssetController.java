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
import java.io.InputStream;
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
        return ResponseEntity.ok(assetService.getAllAssets()); // [cite: 477]
    }

    @PostMapping("/upload")
    public ResponseEntity<AssetDto> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("sellerId") Long sellerId,
            @RequestParam("type") AssetType type) throws Exception {

        AssetDto result = assetService.uploadAndCreateAsset(file, title, sellerId, type); // [cite: 479]
        return new ResponseEntity<>(result, HttpStatus.CREATED); // [cite: 480]
    }

    /**
     * ✅ DOWNLOAD ASSET (Public)
     * Matches SecurityConfig: /api/assets/download/{uuid} OR /api/assets/{uuid}/download
     */
    @GetMapping({"/download/{uuid}", "/{uuid}/download"})
    public ResponseEntity<InputStreamResource> downloadAsset(@PathVariable String uuid) throws Exception {

        // Guard against null strings from frontend [cite: 482]
        if (uuid == null || uuid.trim().isEmpty() || uuid.equalsIgnoreCase("null")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Asset UUID");
        }

        // Fetch metadata from DB [cite: 482]
        Asset asset = assetRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset UUID not found in database"));

        // Attempt to stream physical file from local storage [cite: 483, 524]
        try {
            InputStream stream = storageProvider.read(asset.getFilename());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(asset.getContentType())) // [cite: 483]
                    .body(new InputStreamResource(stream)); // [cite: 483]
        } catch (Exception e) {
            // This is likely where your 404 occurs if the file is missing from ./data/uploads
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Physical file missing on server disk at: " + asset.getFilename());
        }
    }
}