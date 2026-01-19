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
        log.info("Adding to cart: userId={}, designId={}", userId, request.getDesignId());

        // 1. Fetch design from Admin Service (validates existence) [cite: 336]
        DesignDto design = adminServiceClient.getDesignById(request.getDesignId());

        // 2. HARD VALIDATION - Prevent sales of drafts, inactive, or unready designs [cite: 173, 174]
        if (design == null || Boolean.TRUE.equals(design.getDraft()) || !Boolean.TRUE.equals(design.getActive())) {
            throw new DesignNotAvailableException("Design is not available for purchase");
        }

        // 3. SECURE SOURCE OF TRUTH: Block if assetUuid is missing to avoid 500 errors [cite: 257]
        if (design.getAssetUuid() == null) {
            log.error("Design {} has no assetUuid - blocking purchase", design.getId());
            throw new DesignNotAvailableException("Design asset is not ready for download");
        }

        // 4. Check for existing item to update quantity instead of duplicating [cite: 338]
        Optional<CartItem> existingItem = cartItemRepository
                .findByUserIdAndDesignIdAndDeletedFalse(userId, request.getDesignId());

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setPriceCents(design.getFinalPriceCents()); // Lock latest price at add time [cite: 339]
            item.setDesignTitle(design.getTitle());
            item.setAssetUuid(design.getAssetUuid());
            return toResponse(cartItemRepository.save(item));
        }

        // 5. Create new entry using ONLY sale-safe fields [cite: 341, 342]
        CartItem item = CartItem.builder()
                .userId(userId)
                .designId(design.getId())
                .designTitle(design.getTitle())
                .assetUuid(design.getAssetUuid()) // Only use UUID, never internal IDs [cite: 257]
                .priceCents(design.getFinalPriceCents()) // Snapshot price [cite: 342]
                .quantity(request.getQuantity())
                .deleted(false)
                .build();

        log.debug("Saving cart item with assetUuid: {}", design.getAssetUuid());
        return toResponse(cartItemRepository.save(item));
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
            log.info("Soft deleting item {}", cartItemId);
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
        return cartItemRepository.findByUserIdAndDeletedFalse(userId)
                .stream()
                .mapToLong(CartItem::getQuantity)
                .sum();
    }

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