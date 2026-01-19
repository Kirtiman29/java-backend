package com.rdc.asset.controller;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.entity.Asset;
import com.rdc.asset.model.AssetType;
import com.rdc.asset.repo.AssetRepository;
import com.rdc.asset.service.AssetService;
import com.rdc.asset.service.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final AssetService assetService;
    private final AssetRepository assetRepo;
    private final StorageProvider storageProvider;

    @PostMapping("/upload")
    public ResponseEntity<AssetDto> uploadAsset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("sellerId") Long sellerId,
            @RequestParam("type") String type) throws Exception {
        log.info("Processing upload for: {}", title);
        AssetType assetType = AssetType.valueOf(type.toUpperCase());
        AssetDto savedAsset = assetService.uploadAndCreateAsset(file, title, sellerId, assetType);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAsset);
    }

    @GetMapping("/public/{uuid}")
    public ResponseEntity<?> streamPublic(@PathVariable String uuid) throws Exception {
        Asset asset = assetRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found"));

        // Return metadata for TIFFs so Admin validation passes without downloading the whole file
        if (asset.getAssetType() == AssetType.DESIGN_TIFF || asset.getAssetType() == AssetType.MASTER_TIFF) {
            log.info("Returning metadata for restricted asset: {}", uuid);
            return ResponseEntity.ok(new AssetDto(asset.getUuid(), asset.getTitle(), asset.getContentType(), asset.getAssetType()));
        }

        // Stream standard images directly
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .body(new InputStreamResource(storageProvider.read(asset.getFilename())));
    }

    @GetMapping("/download/{uuid}")
    public ResponseEntity<InputStreamResource> downloadProtected(
            @PathVariable String uuid,
            @AuthenticationPrincipal(expression = "#this") Object principal) throws Exception {

        // ✅ FIX: Extract JWT if present, otherwise null.
        // This stops Spring from blocking the request before it hits our logic.
        Jwt jwt = (principal instanceof Jwt) ? (Jwt) principal : null;

        Asset asset = assetRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 🔒 SECURITY CHECK: Only allow TIFF downloads if user is authenticated and paid
        if (asset.getAssetType() == AssetType.DESIGN_TIFF || asset.getAssetType() == AssetType.MASTER_TIFF) {
            if (jwt == null) {
                log.warn("Blocking public request for Master File: {}", uuid);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required to download master files");
            }

            Long userId = Long.parseLong(jwt.getSubject());
            InputStream stream = assetService.getProtectedStream(uuid, userId);
            return serveFile(asset, stream);
        }

        // 🔓 PUBLIC ACCESS: Banners, Category icons, Previews
        // This is safe and allows standard <img> tags to work.
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .body(new InputStreamResource(storageProvider.read(asset.getFilename())));
    }

    private ResponseEntity<InputStreamResource> serveFile(Asset asset, InputStream stream) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + asset.getOriginalFilename() + "\"")
                .body(new InputStreamResource(stream));
    }
}