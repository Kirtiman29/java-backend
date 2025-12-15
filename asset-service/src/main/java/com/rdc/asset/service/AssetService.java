package com.rdc.asset.service;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.dto.AssetRequest;
import java.util.List;

public interface AssetService {

    AssetDto createAsset(AssetRequest request);

    AssetDto getAssetByUuid(String uuid);

    List<AssetDto> getAllAssets();
}
