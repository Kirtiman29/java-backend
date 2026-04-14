package com.rdc.asset.dto;

import com.rdc.asset.model.AssetType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDto {
    private String uuid;
    private String title;
    private String contentType;
    private AssetType assetType;
}