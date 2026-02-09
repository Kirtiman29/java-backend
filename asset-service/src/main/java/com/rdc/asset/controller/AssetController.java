package com.rdc.asset.controller;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.entity.Asset;
import com.rdc.asset.model.AssetType;
import com.rdc.asset.repo.AssetRepository;
import com.rdc.asset.service.AssetService;
import com.rdc.asset.service.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@Slf4j
public class AssetController {

    private final AssetService assetService;
    private final AssetRepository assetRepo;
    private final StorageProvider storageProvider;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @GetMapping
    public ResponseEntity<List<AssetDto>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    /**
     * 🔒 ADMIN UPLOAD: Standard design uploads[cite: 7].
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
     * 🌍 PUBLIC RESUME UPLOAD: Used by Careers Page[cite: 7].
     */
    @PostMapping("/resume-upload")
    public ResponseEntity<AssetDto> uploadResume(@RequestParam("file") MultipartFile file) throws Exception {
        if (!"application/pdf".equals(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF resumes are accepted");
        }

        AssetDto result = assetService.uploadAndCreateAsset(
                file,
                "CANDIDATE_RESUME",
                0L,
                AssetType.DOCUMENT
        );
        return new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    /**
     * ✅ FINALIZED: Secure internal bridge for validation (HEAD) and cleanup (DELETE).
     * Path matches internal bridge route: http://localhost:8090/api/assets/internal/{uuid} [cite: 198-200].
     */
    @RequestMapping(value = "/internal/{uuid}", method = {RequestMethod.HEAD, RequestMethod.DELETE})
    public ResponseEntity<Void> handleInternalAssetRequest(
            @PathVariable String uuid,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key,
            HttpMethod method) {

        validateInternalKey(key);

        if (method == HttpMethod.DELETE) {
            log.info("🗑️ Authorized internal delete request for Asset UUID: {}", uuid);
            assetService.deleteAssetByUuid(uuid); // Purges physical and DB records
            return ResponseEntity.noContent().build();
        } else {
            log.info("🔍 Authorized internal validation request (HEAD) for Asset UUID: {}", uuid);
            assetService.verifyExists(uuid); // Efficient existence check [cite: 202]
            return ResponseEntity.ok().build();
        }
    }

    /**
     * 📥 DOWNLOAD: Permitted to all users for design viewing[cite: 7].
     * ✅ UPDATED: Auto-cleans DB if physical file is missing.
     */
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
            // ✅ CRITICAL FIX: If physical file is missing, purge the orphaned metadata
            log.warn("❌ File missing on disk for asset {}, cleaning DB record", uuid);
            assetService.deleteAssetByUuid(uuid);

            // Return 410 GONE to inform the frontend the resource is permanently removed
            throw new ResponseStatusException(HttpStatus.GONE, "Asset physical file missing and metadata removed");
        }
    }

    /**
     * ✅ Helper method to validate service-to-service bridge keys[cite: 20].
     */
    private void validateInternalKey(String key) {
        if (internalServiceKey == null || !internalServiceKey.equals(key)) {
            log.error("❌ Access Denied: Invalid or missing X-INTERNAL-KEY header");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service key");
        }
    }
}