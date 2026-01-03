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
     * Fetch design by ID from Design Service.
     * Uses PUBLIC endpoint - no authentication required.
     *
     * @param designId Design ID to fetch
     * @return DesignDto with design information
     * @throws DesignNotFoundException if design doesn't exist
     */
    public DesignDto getDesignById(Long designId) {
        // Using PUBLIC endpoint - /api/public/designs/{id}
        String url = designServiceUrl + "/api/public/designs/" + designId;

        log.debug("Fetching design from service: {}", url);

        try {
            DesignDto design = restTemplate.getForObject(url, DesignDto.class);

            if (design == null) {
                throw new DesignNotFoundException("Design not found: " + designId);
            }

            return design;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new DesignNotFoundException("Design not found: " + designId);
            }
            log.error("Error fetching design: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch design", e);
        }
    }

    /**
     * Validate that design exists and is available for wishlist.
     * Design must be:
     * - Not a draft
     * - Active
     *
     * @param designId Design ID to validate
     * @throws DesignNotFoundException if design doesn't exist
     * @throws DesignNotAvailableException if design is draft or inactive
     */
    public void validateDesignForWishlist(Long designId) {
        DesignDto design = getDesignById(designId);

        if (Boolean.TRUE.equals(design.getDraft())) {
            throw new DesignNotAvailableException(
                    "Design '" + design.getTitle() + "' is a draft and cannot be added to wishlist");
        }

        if (Boolean.FALSE.equals(design.getActive())) {
            throw new DesignNotAvailableException(
                    "Design '" + design.getTitle() + "' is not active and cannot be added to wishlist");
        }
    }
}