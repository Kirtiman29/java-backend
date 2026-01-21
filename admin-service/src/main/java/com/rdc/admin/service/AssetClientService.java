package com.rdc.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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

        // ✅ FIXED: Safely extract token from Jwt object to prevent ClassCastException
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String token;
        if (principal instanceof Jwt jwt) {
            token = jwt.getTokenValue();
        } else {
            log.error("Security context principal is not a JWT: {}", principal.getClass().getName());
            throw new RuntimeException("Unauthorized: Valid Admin JWT required for asset validation");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // We call the download endpoint; Asset Service now lets Admins bypass the 403 check
        String url = assetServiceBaseUrl + "/api/assets/download/" + uuid;
        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Asset not found");
            }
        } catch (Exception e) {
            log.error("Asset validation failed for UUID {}: {}", uuid, e.getMessage());
            throw new RuntimeException("Asset Validation Failed: UUID " + uuid + " not found or unauthorized.");
        }
    }
}