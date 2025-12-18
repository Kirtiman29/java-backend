package com.rdc.asset.service;

import com.rdc.asset.dto.AssetDto;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

public interface AssetService {
    // Only multipart upload is allowed for creation
    AssetDto uploadAndCreateAsset(MultipartFile file, String title, String description, Long sellerId) throws IOException;
    AssetDto getAssetByUuid(String uuid);
    List<AssetDto> getAllAssets();
}