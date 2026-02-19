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

    @Override
    @Transactional
    public CartItemResponse addToCart(Long userId, CartItemRequest request) {
        log.info("🛒 Attempting to add to cart: userId={}, designId={}", userId, request.getDesignId());

        DesignDto design = adminServiceClient.getDesignById(request.getDesignId());

        log.debug("📦 Fetched Design Info: ID={}, SKU={}, Title={}, UUID={}",
                design.getId(), design.getDesignIdentifier(), design.getTitle(), design.getAssetUuid());

        // 3. HARD VALIDATION
        if (design == null) {
            throw new DesignNotAvailableException("Design information could not be retrieved");
        }

        if (Boolean.TRUE.equals(design.getDraft()) || !Boolean.TRUE.equals(design.getActive())) {
            log.warn("⚠️ Design {} is either a draft or inactive. Blocking purchase.", design.getId());
            throw new DesignNotAvailableException("Design is not available for purchase");
        }

        if (design.getAssetUuid() == null || design.getAssetUuid().isBlank()) {
            log.error("❌ CRITICAL: Design {} has no assetUuid!", design.getId());
            throw new DesignNotAvailableException("Design asset is not ready for download");
        }

        // 4. Idempotency Check: Update quantity if item already exists
        Optional<CartItem> existingItem = cartItemRepository
                .findByUserIdAndDesignIdAndDeletedFalse(userId, request.getDesignId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            log.info("🔄 Item exists. Updating quantity for CartID: {}", item.getId());
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setPriceCents(design.getFinalPriceCents());
            item.setDesignTitle(design.getTitle());
            item.setAssetUuid(design.getAssetUuid());

            item.setDesignIdentifier(design.getDesignIdentifier());

            return toResponse(cartItemRepository.save(item));
        }

        CartItem newItem = CartItem.builder()
                .userId(userId)
                .designId(design.getId())
                .designIdentifier(design.getDesignIdentifier())
                .designTitle(design.getTitle())
                .assetUuid(design.getAssetUuid())
                .priceCents(design.getFinalPriceCents())
                .quantity(request.getQuantity())
                .deleted(false)
                .build();

        log.info("✅ Successfully saved new cart item with SKU: {}", design.getDesignIdentifier());
        return toResponse(cartItemRepository.save(newItem));
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
    public CartItemResponse updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem item = cartItemRepository.findByIdAndUserIdAndDeletedFalse(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            item.setDeleted(true);
            log.info("🗑️ Removed item {} from cart", cartItemId);
        } else {
            item.setQuantity(quantity);
        }
        return toResponse(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public void removeFromCart(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findByIdAndUserIdAndDeletedFalse(cartItemId, userId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found"));
        item.setDeleted(true);
        cartItemRepository.save(item);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.softDeleteAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getCartItemCount(Long userId) {
        return cartItemRepository.countByUserIdAndDeletedFalse(userId);
    }

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