package com.rdc.order.client;

import com.rdc.order.dto.CartItemDto;
import com.rdc.order.exception.CartServiceException;
import com.rdc.order.exception.EmptyCartException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Client for communicating with Cart Service.
 *
 * ✅ FIXED: Uses actual Cart Service API endpoints
 * ✅ FIXED: Forwards Authorization token from incoming request
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CartServiceClient {

    private final RestTemplate restTemplate;

    @Value("${service.cart.url:http://localhost:8091}")
    private String cartServiceUrl;

    /**
     * Get all cart items for a user.
     * ✅ FIXED: Uses /api/cart/items endpoint with Authorization header
     */
    public List<CartItemDto> getCartItems(Long userId) {
        String url = cartServiceUrl + "/api/cart/items";

        log.info("Fetching cart items from: {}", url);

        try {
            // ✅ Get Authorization token from current request
            HttpHeaders headers = new HttpHeaders();
            String token = getAuthorizationHeader();
            if (token != null) {
                headers.set("Authorization", token);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<List<CartItemDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<CartItemDto>>() {}
            );

            List<CartItemDto> items = response.getBody();

            if (items == null || items.isEmpty()) {
                throw new EmptyCartException("Cart is empty. Add items before checkout.");
            }

            log.info("✅ Fetched {} cart items for user {}", items.size(), userId);
            return items;

        } catch (EmptyCartException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("❌ Error fetching cart: {}", e.getMessage());
            throw new CartServiceException("Failed to fetch cart. Please try again.", e);
        }
    }

    /**
     * Clear all cart items for a user.
     * ✅ FIXED: Uses /api/cart/items endpoint with Authorization header
     */
    public void clearCart(Long userId) {
        String url = cartServiceUrl + "/api/cart/items";

        log.info("Clearing cart via: {}", url);

        try {
            // ✅ Get Authorization token from current request
            HttpHeaders headers = new HttpHeaders();
            String token = getAuthorizationHeader();
            if (token != null) {
                headers.set("Authorization", token);
            }
            HttpEntity<?> entity = new HttpEntity<>(headers);

            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            log.info("✅ Cart cleared for user {}", userId);
        } catch (RestClientException e) {
            log.error("⚠️ Failed to clear cart: {}", e.getMessage());
        }
    }

    /**
     * Extract Authorization header from current HTTP request.
     * This forwards the user's JWT token to the Cart Service.
     */
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