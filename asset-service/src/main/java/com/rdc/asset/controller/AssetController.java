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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    /**
     * ✅ GET ALL ASSETS: Listing functionality
     * Matches: GET http://localhost:8090/api/assets
     */
    @GetMapping
    public ResponseEntity<List<AssetDto>> getAllAssets() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }

    /**
     * ✅ UPLOAD ASSET: Admin only upload
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
     * ✅ DOWNLOAD ASSET: Handles Public Previews vs Paid TIFFs
     */
    @GetMapping("/download/{uuid}")
    public ResponseEntity<InputStreamResource> downloadProtected(
            @PathVariable String uuid,
            @AuthenticationPrincipal(expression = "#this") Object principal) throws Exception {

        // principal is null if the request has no token (permitted by SecurityConfig)
        Jwt jwt = (principal instanceof Jwt) ? (Jwt) principal : null;

        Asset asset = assetRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // 🔒 SECURE PATH: TIFF files require login and payment
        if (asset.getAssetType() == AssetType.TIFF) {
            if (jwt == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login to download master files");
            }

            Long userId = Long.parseLong(jwt.getSubject());
            // getProtectedStream verifies entitlement via Order Service
            InputStream stream = assetService.getProtectedStream(uuid, userId);
            return serveFile(asset, stream);
        }

        // 🔓 PUBLIC PATH: IMAGE, VIDEO, etc. are served to guests
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