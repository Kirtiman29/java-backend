package com.rdc.asset.service;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.entity.Asset;
import com.rdc.asset.model.AssetType;
import com.rdc.asset.repo.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepo;
    private final StorageProvider storageProvider;

    /**
     * Efficiently verifies if an asset exists in the DB.
     * Prevents physical file locking during HEAD requests used by the Admin Service bridge.
     */
    @Override
    @Transactional(readOnly = true)
    public void verifyExists(String uuid) {
        if (!assetRepo.existsByUuid(uuid)) {
            log.error("❌ Validation Failed: Asset UUID {} not found in database", uuid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found");
        }
        log.info("✅ Asset existence verified for UUID: {}", uuid);
    }

    /**
     * ✅ UPDATED: Idempotent Deletion Logic.
     * If the asset is already deleted, the method exits gracefully instead of failing.
     * This ensures retries and purges do not break mid-way.
     */
    @Override
    @Transactional
    public void deleteAssetByUuid(String uuid) {
        log.info("🗑️ Processing deletion for Asset UUID: {}", uuid);

        // 1️⃣ Fetch metadata from DB using Optional for idempotency
        Optional<Asset> optionalAsset = assetRepo.findByUuid(uuid);

        if (optionalAsset.isEmpty()) {
            // ✅ Idempotent check: If it's already gone, don't throw an error
            log.warn("⚠️ Asset {} already deleted or missing from database. Skipping cleanup.", uuid);
            return;
        }

        Asset asset = optionalAsset.get();

        // 2️⃣ Trigger physical file deletion
        try {
            // storageProvider.delete handles missing files gracefully internally
            storageProvider.delete(asset.getFilename());
        } catch (Exception e) {
            // Log the failure but continue with DB cleanup to prevent orphaned records
            log.warn("⚠️ Physical file for asset {} was already removed or inaccessible: {}", uuid, e.getMessage());
        }

        // 3️⃣ Always remove record from database
        assetRepo.delete(asset);

        log.info("✅ Successfully purged Asset UUID: {} from database and storage", uuid);
    }

    /**
     * Handles file uploads and metadata persistence.
     */
    @Override
    @Transactional
    public AssetDto uploadAndCreateAsset(MultipartFile file, String title, Long sellerId, AssetType type) throws Exception {
        String storagePath = "vault/" + type.name() + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        log.info("🚀 Uploading asset: {} to path: {}", title, storagePath);
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

        Asset savedAsset = assetRepo.save(asset);
        log.info("✅ Asset metadata saved with UUID: {}", savedAsset.getUuid());

        return mapToDto(savedAsset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssetDto> getAllAssets() {
        return assetRepo.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private AssetDto mapToDto(Asset a) {
        return AssetDto.builder()
                .uuid(a.getUuid())
                .title(a.getTitle())
                .contentType(a.getContentType())
                .assetType(a.getAssetType())
                .build();
    }
}