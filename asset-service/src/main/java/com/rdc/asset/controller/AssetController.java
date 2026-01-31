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
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    /**
     * 🔒 ADMIN UPLOAD: Standard design uploads
     */
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
     * 🌍 PUBLIC RESUME UPLOAD: Used by Careers Page
     * Fixed: Uses AssetType.DOCUMENT
     */
    @PostMapping("/resume-upload")
    public ResponseEntity<AssetDto> uploadResume(@RequestParam("file") MultipartFile file) throws Exception {
        if (!"application/pdf".equals(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF resumes are accepted");
        }

        // We use sellerId 0L for system-generated/public uploads
        AssetDto result = assetService.uploadAndCreateAsset(
                file,
                "CANDIDATE_RESUME",
                0L,
                AssetType.DOCUMENT // ✅ Ensure this exists in your AssetType enum
        );
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @GetMapping({"/download/{uuid}", "/{uuid}/download"})
    public ResponseEntity<InputStreamResource> downloadAsset(@PathVariable String uuid) throws Exception {
        if (uuid == null || uuid.trim().isEmpty() || uuid.equalsIgnoreCase("null")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Asset UUID");
        }

        Asset asset = assetRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset UUID not found"));

        try {
            InputStream stream = storageProvider.read(asset.getFilename());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(asset.getContentType()))
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Physical file missing on server");
        }
    }
}