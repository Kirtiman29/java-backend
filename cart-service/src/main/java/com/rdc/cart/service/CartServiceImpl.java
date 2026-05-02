package com.rdc.cart.service;

import com.rdc.cart.client.AdminServiceClient;
import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.dto.DesignDto;
import com.rdc.cart.entity.CartItem;
import com.rdc.cart.exception.CartItemNotFoundException;
import com.rdc.cart.exception.DesignNotAvailableException;
import com.rdc.cart.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;
    private final AdminServiceClient adminServiceClient;

    /**
     * Add design to cart
     */
    @Override
    @Transactional
    public CartItemResponse addToCart(Long userId, CartItemRequest request) {

        log.info("🛒 Add to cart request: userId={}, designId={}", userId, request.getDesignId());

        DesignDto design;

        try {
            design = adminServiceClient.getDesignById(request.getDesignId());
        } catch (Exception ex) {
            log.error("❌ Failed to fetch design {} from admin service", request.getDesignId(), ex);
            throw new DesignNotAvailableException("Design information could not be retrieved");
        }

        if (design == null) {
            throw new DesignNotAvailableException("Design does not exist");
        }

        log.debug("📦 Design fetched → ID={}, SKU={}, Title={}",
                design.getId(),
                design.getDesignIdentifier(),
                design.getTitle());

        /**
         * HARD VALIDATION
         */

        if (Boolean.TRUE.equals(design.getDraft())) {
            log.warn("⚠️ Attempt to add draft design {}", design.getId());
            throw new DesignNotAvailableException("Design is not available for purchase");
        }

        if (!Boolean.TRUE.equals(design.getActive())) {
            log.warn("⚠️ Attempt to add inactive design {}", design.getId());
            throw new DesignNotAvailableException("Design is not available for purchase");
        }

        if (design.getAssetUuid() == null || design.getAssetUuid().isBlank()) {
            log.error("❌ Design {} missing assetUuid", design.getId());
            throw new DesignNotAvailableException("Design asset is not ready for download");
        }

        /**
         * DUPLICATE CHECK
         */

        Optional<CartItem> existingItem =
                cartItemRepository.findByUserIdAndDesignIdAndDeletedFalse(userId, request.getDesignId());

        if (existingItem.isPresent()) {
            log.warn("⚠️ Duplicate cart attempt → userId={}, designId={}", userId, request.getDesignId());
            throw new DesignNotAvailableException("This design is already in your cart");
        }

        /**
         * CREATE CART ITEM
         */

        CartItem newItem = CartItem.builder()
                .userId(userId)
                .designId(design.getId())
                .designIdentifier(design.getDesignIdentifier())
                .designTitle(design.getTitle())
                .assetUuid(design.getAssetUuid())
                .priceCents(design.getFinalPriceCents())
                .quantity(1) // digital asset → always 1
                .deleted(false)
                .build();

        CartItem savedItem = cartItemRepository.save(newItem);

        log.info("✅ Cart item created → userId={}, designId={}, cartItemId={}",
                userId,
                design.getId(),
                savedItem.getId());

        return toResponse(savedItem);
    }

    /**
     * Get cart
     */
    @Override
    @Transactional(readOnly = true)
    public List<CartItemResponse> getCartByUserId(Long userId) {

        log.debug("📦 Fetching cart for userId={}", userId);

        return cartItemRepository.findByUserIdAndDeletedFalse(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Update quantity (not really used for digital products)
     */
    @Override
    @Transactional
    public CartItemResponse updateQuantity(Long userId, Long cartItemId, Integer quantity) {

        CartItem item = cartItemRepository
                .findByIdAndUserIdAndDeletedFalse(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

        if (quantity == null || quantity <= 0) {
            item.setDeleted(true);
            log.info("🗑️ Cart item {} removed due to quantity=0", cartItemId);
        } else {
            item.setQuantity(quantity);
            log.info("🔄 Updated quantity for cartItemId={} → {}", cartItemId, quantity);
        }

        return toResponse(cartItemRepository.save(item));
    }

    /**
     * Remove single item
     */
    @Override
    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {

        CartItem item = cartItemRepository
                .findByIdAndUserIdAndDeletedFalse(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

        item.setDeleted(true);

        cartItemRepository.save(item);

        log.info("🗑️ Cart item removed → cartItemId={}, userId={}", cartItemId, userId);
    }

    /**
     * Clear cart
     */
    @Override
    @Transactional
    public void clearCart(Long userId) {

        log.info("🧹 Clearing cart for userId={}", userId);

        cartItemRepository.softDeleteAllByUserId(userId);
    }

    @Override
    @Transactional
    public void removeDesignFromAllCarts(Long designId) {

        log.info("Removing design {} from all active carts", designId);

        cartItemRepository.softDeleteAllByDesignId(designId);
    }

    /**
     * Cart count
     */
    @Override
    @Transactional(readOnly = true)
    public long getCartItemCount(Long userId) {

        return cartItemRepository.countByUserIdAndDeletedFalse(userId);
    }

    /**
     * Mapper
     */
    private CartItemResponse toResponse(CartItem item) {

        return CartItemResponse.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .designId(item.getDesignId())
                .designIdentifier(item.getDesignIdentifier())
                .assetUuid(item.getAssetUuid())
                .designTitle(item.getDesignTitle())
                .quantity(item.getQuantity())
                .priceCents(item.getPriceCents())
                .totalPriceCents(item.getPriceCents() * item.getQuantity())
                .build();
    }
}
