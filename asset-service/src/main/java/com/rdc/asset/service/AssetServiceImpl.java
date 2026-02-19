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

    @Override
    @Transactional(readOnly = true)
    public void verifyExists(String uuid) {
        if (!assetRepo.existsByUuid(uuid)) {
            log.error("❌ Validation Failed: Asset UUID {} not found in database", uuid);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found");
        }
        log.info("✅ Asset existence verified for UUID: {}", uuid);
    }


    @Override
    @Transactional
    public void deleteAssetByUuid(String uuid) {
        log.info("🗑️ Processing deletion for Asset UUID: {}", uuid);

        Optional<Asset> optionalAsset = assetRepo.findByUuid(uuid);

        if (optionalAsset.isEmpty()) {

            log.warn("⚠️ Asset {} already deleted or missing from database. Skipping cleanup.", uuid);
            return;
        }

        Asset asset = optionalAsset.get();

        try {
            storageProvider.delete(asset.getFilename());
        } catch (Exception e) {
            log.warn("⚠️ Physical file for asset {} was already removed or inaccessible: {}", uuid, e.getMessage());
        }

        assetRepo.delete(asset);

        log.info("✅ Successfully purged Asset UUID: {} from database and storage", uuid);
    }

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