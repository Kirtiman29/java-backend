package com.rdc.asset.service;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.entity.Asset;
import com.rdc.asset.model.AssetType;
import com.rdc.asset.repo.AssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepo;
    private final StorageProvider storageProvider;

    @Override
    @Transactional
    public AssetDto uploadAndCreateAsset(MultipartFile file, String title, Long sellerId, AssetType type) throws Exception {
        // Define storage path based on AssetType
        String storagePath = "vault/" + type.name() + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // Persist file to local/cloud storage
        storageProvider.write(storagePath, file.getInputStream());

        // Save metadata to database
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