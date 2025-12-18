package com.rdc.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AssetClientService {

    private final RestTemplate restTemplate;

    @Value("${service.asset.url:http://localhost:8090}")
    private String assetServiceBaseUrl;

    /**
     * Bridges to asset-service to ensure the UUID provided by the
     * Admin UI actually exists before we link it to a Design.
     */
    public void validateAsset(String uuid) {
        if (uuid == null || uuid.isBlank()) return;

        try {
            String url = assetServiceBaseUrl + "/api/assets/" + uuid;
            restTemplate.getForObject(url, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Asset Validation Failed: UUID " + uuid + " not found in Asset Service.");
        }
    }
}