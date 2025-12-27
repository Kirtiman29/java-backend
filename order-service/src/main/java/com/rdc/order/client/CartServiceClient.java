package com.rdc.order.client;

import com.rdc.order.dto.CartItemDto;
import com.rdc.order.exception.CartServiceException;
import com.rdc.order.exception.EmptyCartException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Client for communicating with Cart Service.
 *
 * Used to:
 * 1. Fetch cart items for order creation
 * 2. Clear cart after order is created
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
     *
     * @param userId User ID
     * @return List of cart items with locked prices
     * @throws EmptyCartException if cart is empty
     * @throws CartServiceException if cart service is unavailable
     */
    public List<CartItemDto> getCartItems(Long userId) {
        String url = cartServiceUrl + "/internal/cart/user/" + userId;

        log.info("Fetching cart items from Cart Service: {}", url);

        try {
            ResponseEntity<List<CartItemDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<CartItemDto>>() {}
            );

            List<CartItemDto> items = response.getBody();

            if (items == null || items.isEmpty()) {
                throw new EmptyCartException("Cart is empty. Add items before checkout.");
            }

            log.info("Fetched {} cart items for user {}", items.size(), userId);
            return items;

        } catch (EmptyCartException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Error fetching cart from Cart Service: {}", e.getMessage());
            throw new CartServiceException("Failed to fetch cart. Please try again.", e);
        }
    }

    /**
     * Clear all cart items for a user.
     * Called after order is successfully created.
     *
     * @param userId User ID
     */
    public void clearCart(Long userId) {
        String url = cartServiceUrl + "/internal/cart/user/" + userId;

        log.info("Clearing cart for user {} via Cart Service", userId);

        try {
            restTemplate.delete(url);
            log.info("Cart cleared for user {}", userId);
        } catch (RestClientException e) {
            // Log but don't fail the order - cart clearing is secondary
            log.error("Warning: Failed to clear cart for user {}: {}", userId, e.getMessage());
        }
    }
}
