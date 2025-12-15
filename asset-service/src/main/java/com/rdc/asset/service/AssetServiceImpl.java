package com.rdc.asset.service;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.dto.AssetRequest;
import com.rdc.asset.entity.Asset;
import com.rdc.asset.repo.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepo;

    @Override
    public AssetDto createAsset(AssetRequest req) {

        Asset asset = Asset.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .filename(req.getFilename())
                .contentType(req.getContentType())
                .sizeBytes(req.getSizeBytes())
                .priceCents(req.getPriceCents())
                .sellerId(req.getSellerId())
                .isPublished(true)
                .build();

        Asset saved = assetRepo.save(asset);
        return toDto(saved);
    }

    @Override
    public AssetDto getAssetByUuid(String uuid) {
        return assetRepo.findByUuid(uuid)
                .map(this::toDto)
                .orElseThrow(() -> new RuntimeException("Asset not found!"));
    }

    @Override
    public List<AssetDto> getAllAssets() {
        return assetRepo.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    private AssetDto toDto(Asset a) {
        AssetDto dto = new AssetDto();
        dto.setUuid(a.getUuid());
        dto.setTitle(a.getTitle());
        dto.setDescription(a.getDescription());
        dto.setFilename(a.getFilename());
        dto.setContentType(a.getContentType());
        dto.setSizeBytes(a.getSizeBytes());
        dto.setPriceCents(a.getPriceCents());
        dto.setSellerId(a.getSellerId());
        dto.setIsPublished(a.getIsPublished());
        dto.setUrl("/api/assets/" + a.getUuid() + "/download");
        return dto;
    }
}
