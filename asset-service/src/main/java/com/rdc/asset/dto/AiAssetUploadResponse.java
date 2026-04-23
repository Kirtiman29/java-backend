package com.rdc.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAssetUploadResponse {
    private String assetUuid;
    private String title;
    private String url;
    private String message;
}