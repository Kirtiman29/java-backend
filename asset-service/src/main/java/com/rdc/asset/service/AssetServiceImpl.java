package com.rdc.asset.service;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.entity.Asset;
import com.rdc.asset.repo.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepo;
    private final FileStorageService fileStorageService;

    @Override
    public AssetDto uploadAndCreateAsset(MultipartFile file, String title, String description, Long sellerId) throws IOException {
        // 1. Store the physical file using LocalFileStorageService
        String storedFilename = fileStorageService.store(file);

        // 2. Create Database record with metadata
        Asset asset = Asset.builder()
                .title(title)
                .description(description)
                .filename(storedFilename)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .sellerId(sellerId)
                .isPublished(true) // Default to published for marketplace visibility
                .build();

        Asset saved = assetRepo.save(asset); //
        return toDto(saved);
    }

    @Override
    public AssetDto getAssetByUuid(String uuid) {
        return assetRepo.findByUuid(uuid) //
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Asset not found!")); //
    }

    @Override
    public List<AssetDto> getAllAssets() {
        return assetRepo.findAll().stream().map(this::toDto).toList(); //
    }

    private AssetDto toDto(Asset a) {
        AssetDto dto = new AssetDto();
        dto.setUuid(a.getUuid()); //
        dto.setTitle(a.getTitle()); //
        dto.setDescription(a.getDescription()); //
        dto.setContentType(a.getContentType()); //
        dto.setSizeBytes(a.getSizeBytes()); //
        // Provides the URL for the download/preview endpoint
        dto.setUrl("/api/assets/" + a.getUuid() + "/download");
        return dto;
    }
}