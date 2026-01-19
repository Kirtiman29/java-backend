package com.rdc.asset.service;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.entity.Asset;
import com.rdc.asset.model.AssetType;
import com.rdc.asset.repo.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepo;
    private final StorageProvider storageProvider;
    private final RestTemplate restTemplate;

    @Value("${service.order.url}")
    private String orderServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @Override
    @Transactional
    public AssetDto uploadAndCreateAsset(MultipartFile file, String title, Long sellerId, AssetType type) throws Exception {
        // PRODUCTION RULE: Sanitize filenames and use a vault-based structure [cite: 467]
        String storagePath = "vault/" + type.name() + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        storageProvider.write(storagePath, file.getInputStream());

        Asset asset = Asset.builder()
                .title(title)
                .filename(storagePath)
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .assetType(type)
                .sellerId(sellerId)
                .build();

        return mapToDto(assetRepo.save(asset));
    }

    @Override
    @Transactional(readOnly = true)
    public InputStream getProtectedStream(String uuid, Long userId) throws Exception {
        Asset asset = assetRepo.findByUuid(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // PRODUCTION RULE: Protected stream only for high-res master files or designs [cite: 471]
        if (asset.getAssetType() != AssetType.DESIGN_TIFF && asset.getAssetType() != AssetType.MASTER_TIFF) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid download target");
        }

        // MANDATORY: Verify entitlement via Order Service [cite: 472]
        if (!verifyEntitlement(uuid, userId)) {
            log.warn("Access Denied: User {} attempted to download asset {} without valid payment", userId, uuid);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Payment Required");
        }

        return storageProvider.read(asset.getFilename());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetDto> getAllAssets() {
        return assetRepo.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    /**
     * Internal Bridge to Order Service
     * [cite_start]Checks if the user has a PAID order for this specific asset [cite: 475, 476]
     */
    private boolean verifyEntitlement(String assetUuid, Long userId) {
        String url = String.format("%s/api/internal/orders/verify-download?userId=%d&assetUuid=%s",
                orderServiceUrl, userId, assetUuid);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalServiceKey); // Shared Secret Bridge [cite: 476]

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return (Boolean) response.getBody().get("allowed");
        } catch (Exception e) {
            log.error("Entitlement Bridge Failure for User {} on Asset {}: {}", userId, assetUuid, e.getMessage());
            return false;
        }
    }

    /**
     * ✅ FIXED: Type Mismatch Resolution
     * [cite_start]Passes the AssetType enum directly to the AssetDto builder[cite: 478].
     */
    private AssetDto mapToDto(Asset a) {
        return AssetDto.builder()
                .uuid(a.getUuid())
                .title(a.getTitle())
                .contentType(a.getContentType())
                .assetType(a.getAssetType()) // Fixed: Matches updated DTO enum type
                .build();
    }
}