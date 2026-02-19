package com.rdc.cart.client;

import com.rdc.cart.dto.DesignDto;
import com.rdc.cart.exception.DesignNotAvailableException;
import com.rdc.cart.exception.DesignNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.admin.url}")
    private String adminServiceUrl;

    public DesignDto getDesignById(Long designId) {
        String url = adminServiceUrl + "/api/public/designs/" + designId;
        log.info("📡 Internal Fetch: Synchronizing design metadata from Admin Service: {}", url);

        try {
            // Internal calls use a simple GET to the public endpoint
            ResponseEntity<DesignDto> response = restTemplate.getForEntity(url, DesignDto.class);

            DesignDto design = response.getBody();
            if (design == null) {
                throw new DesignNotFoundException("Design not found: " + designId);
            }

            validateDesignAvailability(design);
            return design;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new DesignNotFoundException("Design ID " + designId + " does not exist in Admin DB.");
            }
            log.error("❌ Admin Service communication failed: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch design from Admin Service", e);
        }
    }

    private void validateDesignAvailability(DesignDto design) {
        if (Boolean.TRUE.equals(design.getDraft())) {
            throw new DesignNotAvailableException(
                    "Design '" + design.getTitle() + "' is currently in draft and cannot be purchased.");
        }

        if (Boolean.FALSE.equals(design.getActive())) {
            throw new DesignNotAvailableException(
                    "Design '" + design.getTitle() + "' is not active.");
        }

        if (design.getFinalPriceCents() == null || design.getFinalPriceCents() < 0) {
            throw new DesignNotAvailableException(
                    "Design '" + design.getTitle() + "' has an invalid price configuration.");
        }
    }
}