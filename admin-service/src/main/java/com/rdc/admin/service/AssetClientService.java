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

    @Value("${service.asset.url}")
    private String assetServiceBaseUrl;

    @Value("${internal.service.key}")
    private String internalKey;

    public void validateAsset(String uuid) {
        if (uuid == null || uuid.isBlank()) return;

        String url = assetServiceBaseUrl + "/api/assets/internal/assets/" + uuid;
        log.info("Validating asset via internal bridge: {}", url);

        try {
            restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    new HttpEntity<>(buildInternalHeaders()),
                    Void.class
            );
            log.info("Asset validated successfully: {}", uuid);
        } catch (Exception e) {
            log.warn("Asset {} no longer exists or validation failed, skipping reference update", uuid);
        }
    }

    public AssetResponse upload(MultipartFile file, AssetType type) {
        String url = assetServiceBaseUrl + "/api/assets/internal/upload";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-INTERNAL-KEY", internalKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());
        body.add("title", file.getOriginalFilename());
        body.add("sellerId", 0L);
        body.add("type", type.name());

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, request, AssetResponse.class).getBody();
    }

    public void deleteAsset(String uuid) {
        if (uuid == null || uuid.isBlank()) return;

        //Uses injected URL for authorized deletion
        String url = assetServiceBaseUrl + "/api/assets/internal/assets/" + uuid;
        log.info("🗑️ Sending authorized internal delete request for UUID: {}", uuid);

        try {
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

    private HttpHeaders buildInternalHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-KEY", internalKey);
        return headers;
    }

}
