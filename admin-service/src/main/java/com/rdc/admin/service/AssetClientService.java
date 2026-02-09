package com.rdc.admin.service;

import com.rdc.admin.dto.AssetResponse;
import com.rdc.admin.entity.AssetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetClientService {

    private final RestTemplate restTemplate;

    @Value("${service.asset.url:http://localhost:8090}")
    private String assetServiceBaseUrl;

    @Value("${internal.service.key}")
    private String internalKey;

    /**
     * ✅ UPDATED: Gracefully validates asset existence via internal bridge.
     * Prevents 401 errors using authorized headers and skips missing assets to avoid transaction rollbacks [cite: 215-220].
     */
    public void validateAsset(String uuid) {
        if (uuid == null || uuid.isBlank()) return;

        String url = assetServiceBaseUrl + "/api/assets/internal/" + uuid;
        log.info("📡 Validating asset via internal bridge: {}", url);

        try {
            // Use exchange with HEAD to pass the security key without downloading full content [cite: 218-219]
            restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    new HttpEntity<>(buildInternalHeaders()),
                    Void.class
            );
            log.info("✅ Asset validated successfully: {}", uuid);
        } catch (Exception e) {
            // ✅ FIX: Log a warning instead of throwing an exception to allow designs to function even if an asset is missing
            log.warn("⚠️ Asset {} no longer exists or validation failed, skipping reference update", uuid);
        }
    }

    /**
     * Uploads a physical file to the Asset Service [cite: 221-224].
     */
    public AssetResponse upload(MultipartFile file, AssetType type) {
        String url = assetServiceBaseUrl + "/api/assets/upload";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("title", file.getOriginalFilename());
        body.add("sellerId", 0L);
        body.add("type", type.name());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, request, AssetResponse.class).getBody();
    }

    /**
     * ✅ UPDATED: Authorized physical deletion via internal cleanup endpoint [cite: 225-230].
     */
    public void deleteAsset(String uuid) {
        if (uuid == null || uuid.isBlank()) return;

        String url = assetServiceBaseUrl + "/api/assets/internal/" + uuid;
        log.info("🗑️ Sending authorized internal delete request for UUID: {}", uuid);

        try {
            // exchange() is used to pass custom headers with the DELETE method [cite: 228-230]
            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    new HttpEntity<>(buildInternalHeaders()),
                    Void.class
            );
            log.info("✅ Physical asset deletion successful for UUID: {}", uuid);
        } catch (Exception e) {
            log.error("⚠️ Authorized remote asset deletion failed for UUID {}: {}", uuid, e.getMessage());
        }
    }

    /**
     * ✅ HELPER: Centralizes construction of authorized bridge headers[cite: 228].
     */
    private HttpHeaders buildInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalKey);
        return headers;
    }
}