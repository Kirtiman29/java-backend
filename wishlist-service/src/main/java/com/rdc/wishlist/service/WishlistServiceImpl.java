package com.rdc.wishlist.service;

import com.rdc.wishlist.client.DesignClientService;
import com.rdc.wishlist.client.OrderClientService;
import com.rdc.wishlist.dto.DesignDto;
import com.rdc.wishlist.dto.WishlistResponse;
import com.rdc.wishlist.entity.Wishlist;
import com.rdc.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository repository;
    private final DesignClientService designClientService;
    private final OrderClientService orderClientService;
    @Override
    @Transactional
    public void addToWishlist(Long userId, Long designId) {
        log.info("Adding design {} to wishlist for user {}", designId, userId);

        try {

            designClientService.validateDesignForWishlist(designId);

            if (!repository.existsByUserIdAndDesignId(userId, designId)) {
                Wishlist item = Wishlist.builder()
                        .userId(userId)
                        .designId(designId)
                        .build();
                repository.save(item);
                log.info("✅ Design {} successfully added to wishlist for user {}", designId, userId);
            }
        } catch (Exception e) {
            log.error("❌ Error adding to wishlist: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getUserWishlist(Long userId) {

        log.info("Fetching wishlist for user {}", userId);

        List<Wishlist> items = repository.findByUserId(userId);

        return items.stream()
                .map(item -> {

                    try {

                        DesignDto design = designClientService.getDesignById(item.getDesignId());

                        // ⭐ NEW CHECK: purchased design
                        boolean purchased = orderClientService
                                .hasUserPurchased(userId, design.getAssetUuid());

                        if (purchased) {
                            return null; // purchased item skip
                        }

                        return WishlistResponse.builder()
                                .designId(item.getDesignId())
                                .title(design.getTitle() != null ? design.getTitle() : "Unknown Design")
                                .slug(design.getSlug())
                                .assetUuid(design.getAssetUuid() != null ? design.getAssetUuid() : "placeholder-uuid")
                                .basePriceCents(design.getBasePriceCents() != null ? design.getBasePriceCents() : 0L)
                                .finalPriceCents(design.getFinalPriceCents() != null ? design.getFinalPriceCents() : 0L)
                                .discountPercent(design.getDiscountPercent() != null ? design.getDiscountPercent() : 0)
                                .specialOffer(Boolean.TRUE.equals(design.getSpecialOffer()))
                                .build();

                    } catch (Exception e) {

                        log.warn("⚠️ Data mismatch: Could not map design {}: {}",
                                item.getDesignId(), e.getMessage());

                        return WishlistResponse.builder()
                                .designId(item.getDesignId())
                                .title("Item Unavailable")
                                .build();
                    }

                })

                // ⭐ important
                .filter(Objects::nonNull)

                .collect(Collectors.toList());
    }
    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long designId) {
        log.info("Removing design {} from wishlist for user {}", designId, userId);
        repository.deleteByUserIdAndDesignId(userId, designId);
    }

    @Override
    public boolean isWishlisted(Long userId, Long designId) {
        return repository.existsByUserIdAndDesignId(userId, designId);
    }
}