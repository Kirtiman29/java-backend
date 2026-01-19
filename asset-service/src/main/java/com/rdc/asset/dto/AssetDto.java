package com.rdc.asset.dto;

import com.rdc.asset.model.AssetType; // ✅ Add this import
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDto {
    private String uuid;
    private String title;
    private String contentType;
    private AssetType assetType; // ✅ Change from String to AssetType
}