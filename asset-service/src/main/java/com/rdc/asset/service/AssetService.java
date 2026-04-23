package com.rdc.asset.service;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.dto.AiAssetUploadResponse;
import com.rdc.asset.model.AssetType;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.List;

public interface AssetService {
    AssetDto uploadAndCreateAsset(MultipartFile file, String title, Long sellerId, AssetType type) throws Exception;
    AiAssetUploadResponse uploadAiInput(MultipartFile file, String title, Long userId) throws Exception;
    List<AssetDto> getAllAssets(); // Ensure this exists
    void deleteAssetByUuid(String uuid);
    void verifyExists(String uuid);
}
