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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.admin.url:http://localhost:8080}")
    private String adminServiceUrl;

    public DesignDto getDesignById(Long designId) {
        String url = adminServiceUrl + "/api/designs/" + designId; // ✅ Match Admin Controller path
        log.info("Fetching design from Admin Service with token forwarding: {}", url);

        try {
            // ✅ FORWARD THE TOKEN
            HttpHeaders headers = new HttpHeaders();
            String token = getAuthorizationHeader();
            if (token != null) {
                headers.set("Authorization", token);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // ✅ Use exchange to send headers
            ResponseEntity<DesignDto> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, DesignDto.class);

            DesignDto design = response.getBody();
            if (design == null) {
                throw new DesignNotFoundException("Design not found: " + designId);
            }

            validateDesignAvailability(design);
            return design;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                log.error("❌ Admin Service rejected request. Path might be protected or token invalid.");
            }
            throw new RuntimeException("Failed to fetch design from Admin Service", e);
        }
    }

    private String getAuthorizationHeader() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader("Authorization");
        }
        return null;
    }

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