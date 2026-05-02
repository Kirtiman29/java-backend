package com.rdc.wishlist.client;

import com.rdc.wishlist.dto.DesignDto;
import com.rdc.wishlist.exception.DesignNotAvailableException;
import com.rdc.wishlist.exception.DesignNotFoundException;
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
public class DesignClientService {

    private final RestTemplate restTemplate;

    @Value("${service.design.url}")
    private String designServiceUrl;

    public DesignDto getDesignById(Long designId) {
        String url = designServiceUrl + "/api/public/designs/" + designId;
        log.debug("Requesting metadata from Admin Service: {}", url);

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

            return design;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new DesignNotFoundException("Design metadata not found for ID: " + designId);
            }
            if (e.getStatusCode() == HttpStatus.FORBIDDEN || e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new DesignNotAvailableException("Design is not available for this user.");
            }

            log.error("Admin Service error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Communication failure with Admin Service");
        } catch (Exception e) {
            log.error("Critical fetch failure: {}", e.getMessage());
            throw new RuntimeException("Internal Service Communication Error");
        }
    }

    public void validateDesignForWishlist(Long designId) {
        DesignDto design = getDesignById(designId);

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
