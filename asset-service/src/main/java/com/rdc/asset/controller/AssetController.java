package com.rdc.asset.controller;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.service.AssetService;
import com.rdc.asset.repo.AssetRepository;
import com.rdc.asset.entity.Asset;
import com.rdc.asset.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetRepository assetRepo;
    private final FileStorageService fileStorageService;

    // Multipart Upload Endpoint for Admin [cite: 10]
    @PostMapping("/upload")
    public ResponseEntity<AssetDto> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("sellerId") Long sellerId
    ) throws IOException {
        return ResponseEntity.ok(assetService.uploadAndCreateAsset(file, title, description, sellerId));
    }

    // Download/Preview Endpoint - used by Admin Service for previews [cite: 11, 12]
    @GetMapping("/{uuid}/download")
    public ResponseEntity<byte[]> downloadAsset(@PathVariable String uuid) throws IOException {
        Asset asset = assetRepo.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Asset not found"));

        byte[] data = fileStorageService.readAllBytes(asset.getFilename()); // [cite: 12, 52]

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, asset.getContentType()) // [cite: 12]
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + asset.getFilename() + "\"") // [cite: 12]
                .body(data);
    }

    // Metadata endpoint - used for Admin Service bridge validation [cite: 13, 303, 322]
    @GetMapping("/{uuid}")
    public ResponseEntity<AssetDto> getAsset(@PathVariable String uuid) {
        return ResponseEntity.ok(assetService.getAssetByUuid(uuid));
    }

    @GetMapping
    public ResponseEntity<List<AssetDto>> getAll() {
        return ResponseEntity.ok(assetService.getAllAssets()); // [cite: 14, 42]
    }
}