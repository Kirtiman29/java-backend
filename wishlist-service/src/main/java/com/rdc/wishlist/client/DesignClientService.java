package com.rdc.wishlist.client;

import com.rdc.wishlist.dto.DesignDto;
import com.rdc.wishlist.exception.DesignNotAvailableException;
import com.rdc.wishlist.exception.DesignNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for fetching design information from Admin Service.
 * Uses PUBLIC endpoint - no authentication required.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DesignClientService {

    private final RestTemplate restTemplate;

    @Value("${service.design.url}")
    private String designServiceUrl;

    /**
     * ✅ Fetch design by ID from Admin Service.
     * Uses PUBLIC endpoint: /api/public/designs/{id}
     */
    public DesignDto getDesignById(Long designId) {
        String url = designServiceUrl + "/api/public/designs/" + designId;

        log.debug("📡 Requesting metadata from Admin Service: {}", url);

        try {
            DesignDto design = restTemplate.getForObject(url, DesignDto.class);

            if (design == null) {
                throw new DesignNotFoundException("Design not found: " + designId);
            }

            return design;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new DesignNotFoundException("Design metadata not found for ID: " + designId);
            }
            log.error("❌ Admin Service error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Communication failure with Admin Service");
        } catch (Exception e) {
            log.error("❌ Critical fetch failure: {}", e.getMessage());
            throw new RuntimeException("Internal Service Communication Error");
        }
    }

    /**
     * ✅ Validate that design is live and active before wishlisting.
     */
    public void validateDesignForWishlist(Long designId) {
        DesignDto design = getDesignById(designId);

        // ✅ FIXED: Null-safe check for Title to prevent crashes
        String designTitle = design.getTitle() != null ? design.getTitle() : "ID: " + designId;

        if (Boolean.TRUE.equals(design.getDraft())) {
            throw new DesignNotAvailableException(
                    "Design '" + designTitle + "' is a draft and cannot be saved.");
        }

        if (Boolean.FALSE.equals(design.getActive())) {
            throw new DesignNotAvailableException(
                    "Design '" + designTitle + "' is currently inactive.");
        }
    }
}