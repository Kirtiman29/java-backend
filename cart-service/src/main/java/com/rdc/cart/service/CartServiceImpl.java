package com.rdc.cart.service;

import com.rdc.cart.client.AdminServiceClient;
import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.dto.DesignDto;
import com.rdc.cart.entity.CartItem;
import com.rdc.cart.exception.CartItemNotFoundException;
import com.rdc.cart.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Cart Service Implementation
 *
 * SECURITY NOTES:
 * 1. Price is ALWAYS fetched from Admin Service - NEVER from frontend request
 * 2. userId comes from auth header (X-User-Id) - NEVER from request body
 * 3. User can only access/modify their own cart items
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final AdminServiceClient adminServiceClient;

    @Override
    @Transactional
    public CartItemResponse addToCart(Long userId, CartItemRequest request) {
        log.info("Adding design {} to cart for user {}", request.getDesignId(), userId);

        // 1. Fetch design from Admin Service (validates existence + availability)
        DesignDto design = adminServiceClient.getDesignById(request.getDesignId());

        // 2. Check if design already exists in cart
        Optional<CartItem> existingItem = cartItemRepository
                .findByUserIdAndDesignIdAndDeletedFalse(userId, request.getDesignId());

        if (existingItem.isPresent()) {
            // Update quantity instead of adding duplicate
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            // Update price snapshot to current price
            item.setPriceCents(design.getFinalPriceCents());
            item.setDesignTitle(design.getTitle());
            item.setAssetUuid(design.getAssetUuid());

            CartItem saved = cartItemRepository.save(item);
            log.info("Updated existing cart item {} with new quantity {}", saved.getId(), saved.getQuantity());
            return toResponse(saved);
        }

        // 3. Create new cart item with price from Admin Service
        CartItem item = CartItem.builder()
                .userId(userId)
                .designId(request.getDesignId())
                .assetUuid(design.getAssetUuid())
                .designTitle(design.getTitle())
                .quantity(request.getQuantity())
                .priceCents(design.getFinalPriceCents())  // Price from Admin Service, NOT from request!
                .deleted(false)
                .build();

        CartItem saved = cartItemRepository.save(item);
        log.info("Created new cart item {} for design {}", saved.getId(), saved.getDesignId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponse> getCartByUserId(Long userId) {
        log.debug("Fetching cart for user {}", userId);

        return cartItemRepository.findByUserIdAndDeletedFalse(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CartItemResponse updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        log.info("Updating quantity for cart item {} to {} for user {}", cartItemId, quantity, userId);

        CartItem item = cartItemRepository.findByIdAndUserIdAndDeletedFalse(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item not found: " + cartItemId));

        if (quantity <= 0) {
            // Remove item if quantity is 0 or negative
            item.setDeleted(true);
            cartItemRepository.save(item);
            log.info("Removed cart item {} (quantity was {})", cartItemId, quantity);
            return toResponse(item);
        }

        item.setQuantity(quantity);
        CartItem saved = cartItemRepository.save(item);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        log.info("Removing cart item {} for user {}", cartItemId, userId);

        CartItem item = cartItemRepository.findByIdAndUserIdAndDeletedFalse(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException(
                        "Cart item not found: " + cartItemId));

        // Soft delete
        item.setDeleted(true);
        cartItemRepository.save(item);

        log.info("Soft deleted cart item {}", cartItemId);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        log.info("Clearing cart for user {}", userId);

        int deletedCount = cartItemRepository.softDeleteAllByUserId(userId);

        log.info("Cleared {} items from cart for user {}", deletedCount, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCartItemCount(Long userId) {
        return cartItemRepository.countByUserIdAndDeletedFalse(userId);
    }

    /**
     * Map entity to response DTO.
     */
    private CartItemResponse toResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .designId(item.getDesignId())
                .assetUuid(item.getAssetUuid())
                .designTitle(item.getDesignTitle())
                .quantity(item.getQuantity())
                .priceCents(item.getPriceCents())
                .totalPriceCents(item.getPriceCents() * item.getQuantity())
                .build();
    }
}
