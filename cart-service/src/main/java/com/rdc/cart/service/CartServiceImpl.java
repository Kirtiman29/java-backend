package com.rdc.cart.service;

import com.rdc.cart.dto.CartItemRequest;
import com.rdc.cart.dto.CartItemResponse;
import com.rdc.cart.entity.CartItem;
import com.rdc.cart.repository.CartItemRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartItemRepository cartItemRepository;

    @Override
    @Transactional
    public CartItemResponse addToCart(CartItemRequest request) {
        CartItem item = CartItem.builder()
                .userId(request.getUserId())
                .assetId(request.getAssetId())
                .assetUuid(request.getAssetUuid())
                .quantity(request.getQuantity())
                .priceCents(request.getPriceCents())
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
        resp.setAssetId(item.getAssetId());
        resp.setAssetUuid(item.getAssetUuid());
        resp.setQuantity(item.getQuantity());
        resp.setPriceCents(item.getPriceCents());
        return resp;
    }
}
