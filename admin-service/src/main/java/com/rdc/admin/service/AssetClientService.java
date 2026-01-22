package com.rdc.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetClientService {

    private final RestTemplate restTemplate;

    @Value("${service.asset.url:http://localhost:8090}")
    private String assetServiceBaseUrl;

    /**
     * Validates that an asset exists in the Asset Service.
     * Since TIFF protection is removed, this is now a simple public check.
     */
    public void validateAsset(String uuid) {
        if (uuid == null || uuid.isBlank()) return;

        String url = assetServiceBaseUrl + "/api/assets/download/" + uuid;

        try {
            // Simplified: No JWT needed as download endpoint is now public for previews
            ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Asset not found");
            }
        } catch (Exception e) {
            log.error("Asset validation failed for UUID {}: {}", uuid, e.getMessage());
            throw new RuntimeException("Asset Validation Failed: UUID " + uuid + " not found.");
        }
    }
}