package com.rdc.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetClientService {

    private final RestTemplate restTemplate;

    @Value("${service.asset.url:http://localhost:8090}")
    private String assetServiceBaseUrl;

    public void validateAsset(String uuid) {
        if (uuid == null || uuid.isBlank()) return;

        String url = assetServiceBaseUrl + "/api/assets/public/" + uuid;
        try {
            // ✅ FIX: Use byte[] to accept any content type (JSON or Binary)
            ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Asset validation failed status");
            }
        } catch (Exception e) {
            log.error("Asset validation failed for UUID {}: {}", uuid, e.getMessage());
            throw new RuntimeException("Asset Validation Failed: UUID " + uuid + " not found.");
        }
    }
}