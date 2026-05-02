package com.rdc.cart.client;

import com.rdc.cart.dto.DesignDto;
import com.rdc.cart.exception.DesignNotAvailableException;
import com.rdc.cart.exception.DesignNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.admin.url}")
    private String adminServiceUrl;

    public DesignDto getDesignById(Long designId) {
        String url = adminServiceUrl + "/api/public/designs/" + designId;
        log.info("Requesting design metadata from Admin Service: {}", url);

        try {
            HttpHeaders headers = new HttpHeaders();
            String token = getAuthorizationHeader();
            if (token != null) {
                headers.set("Authorization", token);
            }

            ResponseEntity<DesignDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    DesignDto.class
            );

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
            if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new DesignNotAvailableException("Design is not available for this user.");
            }

            log.error("Admin Service communication failed: {}", e.getMessage());
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

    private String getAuthorizationHeader() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader("Authorization");
        }

        return null;
    }
}
