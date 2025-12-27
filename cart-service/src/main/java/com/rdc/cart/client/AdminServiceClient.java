package com.rdc.cart.client;

import com.rdc.cart.dto.DesignDto;
import com.rdc.cart.exception.DesignNotAvailableException;
import com.rdc.cart.exception.DesignNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Client for communicating with Admin Service.
 * Fetches design information including price.
 *
 * Uses the PUBLIC endpoint (no auth required)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.admin.url:http://localhost:8080}")
    private String adminServiceUrl;

    /**
     * Fetch design by ID from Admin Service.
     * Uses PUBLIC endpoint - no authentication required.
     *
     * @param designId Design ID to fetch
     * @return DesignDto with all design information including price
     * @throws DesignNotFoundException if design doesn't exist
     * @throws DesignNotAvailableException if design is draft or inactive
     */
    public DesignDto getDesignById(Long designId) {
        // Using PUBLIC endpoint - no auth needed
        String url = adminServiceUrl + "/api/public/designs/" + designId;

        log.info("Fetching design from Admin Service: {}", url);

        try {
            DesignDto design = restTemplate.getForObject(url, DesignDto.class);

            if (design == null) {
                throw new DesignNotFoundException("Design not found: " + designId);
            }

            // Validate design is available for purchase
            validateDesignAvailability(design);

            return design;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new DesignNotFoundException("Design not found: " + designId);
            }
            log.error("Error fetching design from Admin Service: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch design from Admin Service", e);
        }
    }

    /**
     * Validate that design can be added to cart.
     * Design must be:
     * - Not a draft
     * - Active
     * - Have a valid price
     */
    private void validateDesignAvailability(DesignDto design) {
        if (Boolean.TRUE.equals(design.getDraft())) {
            throw new DesignNotAvailableException(
                    "Design '" + design.getTitle() + "' is a draft and cannot be purchased");
        }

        if (Boolean.FALSE.equals(design.getActive())) {
            throw new DesignNotAvailableException(
                    "Design '" + design.getTitle() + "' is not active and cannot be purchased");
        }

        if (design.getFinalPriceCents() == null || design.getFinalPriceCents() < 0) {
            throw new DesignNotAvailableException(
                    "Design '" + design.getTitle() + "' does not have a valid price");
        }
    }
}