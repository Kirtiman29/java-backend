// File: src/main/java/com/rdc/cart/service/CartServiceImpl.java
package com.rdc.cart.service;

import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.entity.CartItem;
import com.rdc.cart.repository.CartItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final RestTemplate restTemplate; // Added to communicate with Admin Service

    @Override
    @Transactional
    public CartItemResponse addToCart(CartItemRequest request) {
        // Fetch Design details from Admin Service
        String adminUrl = "http://localhost:8080/api/admin/designs/" + request.getDesignId();

        // We use Map to quickly access fields from the Admin Design response
        Map<String, Object> design = restTemplate.getForObject(adminUrl, Map.class);

        if (design == null) {
            throw new EntityNotFoundException("Design not found in Admin Service");
        }

        // Validate business rules: Cannot add if inactive or a draft
        boolean active = (boolean) design.get("active");
        boolean draft = (boolean) design.get("draft");
        if (!active || draft) {
            throw new RuntimeException("This design is currently unavailable");
        }

        CartItem item = CartItem.builder()
                .userId(request.getUserId())
                .designId(request.getDesignId())
                .assetUuid((String) design.get("assetUuid"))
                .quantity(request.getQuantity())
                .priceCents(((Number) design.get("finalPriceCents")).longValue()) // Lock verified price
                .deleted(false)
                .build();

        CartItem saved = cartItemRepository.save(item);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponse> getCartByUserId(Long userId) {
        return cartItemRepository.findByUserIdAndDeletedFalse(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void removeFromCart(Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new EntityNotFoundException("Cart item not found: " + cartItemId));

        item.setDeleted(true);
        item.setUpdatedAt(LocalDateTime.now());
        cartItemRepository.save(item);
    }

    private CartItemResponse toResponse(CartItem item) {
        CartItemResponse resp = new CartItemResponse();
        resp.setId(item.getId());
        resp.setUserId(item.getUserId());
        resp.setAssetId(item.getDesignId()); // Now returning Design ID
        resp.setAssetUuid(item.getAssetUuid());
        resp.setQuantity(item.getQuantity());
        resp.setPriceCents(item.getPriceCents());
        return resp;
    }
}