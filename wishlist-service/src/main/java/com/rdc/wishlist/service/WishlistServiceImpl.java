package com.rdc.wishlist.service;

import com.rdc.wishlist.client.DesignClientService;
import com.rdc.wishlist.client.OrderClientService;
import com.rdc.wishlist.dto.DesignDto;
import com.rdc.wishlist.dto.WishlistResponse;
import com.rdc.wishlist.entity.Wishlist;
import com.rdc.wishlist.exception.DesignNotAvailableException;
import com.rdc.wishlist.exception.DesignNotFoundException;
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
            DesignDto design = designClientService.fetchDesignForWishlist(designId);

            if (!repository.existsByUserIdAndDesignId(userId, designId)) {
                Wishlist item = Wishlist.builder()
                        .userId(userId)
                        .designId(designId)
                        .designIdentifier(design.getDesignIdentifier())
                        .designTitle(design.getTitle())
                        .assetUuid(design.getAssetUuid())
                        .build();
                repository.save(item);
                log.info("Design {} successfully added to wishlist for user {}", designId, userId);
            }
        } catch (Exception e) {
            log.error("Error adding to wishlist: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    @Transactional
    public List<WishlistResponse> getUserWishlist(Long userId) {
        log.info("Fetching wishlist for user {}", userId);

        List<Wishlist> items = repository.findByUserId(userId);

        return items.stream()
                .map(item -> mapWishlistItem(userId, item))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private WishlistResponse mapWishlistItem(Long userId, Wishlist item) {
        try {
            log.info("Resolving wishlist item metadata: userId={}, designId={}", userId, item.getDesignId());

            DesignDto design = designClientService.getDesignById(item.getDesignId());

            item.setDesignIdentifier(design.getDesignIdentifier());
            item.setDesignTitle(design.getTitle());
            item.setAssetUuid(design.getAssetUuid());

            String assetUuid = firstNonBlank(design.getAssetUuid(), item.getAssetUuid());
            if (assetUuid != null) {
                boolean purchased = orderClientService.hasUserPurchased(userId, assetUuid);
                if (purchased) {
                    repository.deleteByUserIdAndDesignId(userId, item.getDesignId());
                    return null;
                }
            }

            return buildResponse(item, design);
        } catch (DesignNotFoundException | DesignNotAvailableException e) {
            log.warn("Keeping wishlist entry for user {} and design {} after metadata lookup failure: {}",
                    userId, item.getDesignId(), e.getMessage());
            return buildResponse(item, null);
        } catch (Exception e) {
            log.warn("Keeping wishlist entry for user {} and design {} after temporary mapping error: {}",
                    userId, item.getDesignId(), e.getMessage());
            return buildResponse(item, null);
        }
    }

    private WishlistResponse buildResponse(Wishlist item, DesignDto design) {
        String title = firstNonBlank(
                design != null ? design.getTitle() : null,
                item.getDesignTitle(),
                "Unknown Design"
        );
        String slug = design != null ? design.getSlug() : null;
        String assetUuid = firstNonBlank(
                design != null ? design.getAssetUuid() : null,
                item.getAssetUuid(),
                "placeholder-uuid"
        );

        Long basePriceCents = design != null && design.getBasePriceCents() != null
                ? design.getBasePriceCents()
                : 0L;
        Long finalPriceCents = design != null && design.getFinalPriceCents() != null
                ? design.getFinalPriceCents()
                : 0L;
        Integer discountPercent = design != null && design.getDiscountPercent() != null
                ? design.getDiscountPercent()
                : 0;
        boolean specialOffer = design != null && Boolean.TRUE.equals(design.getSpecialOffer());

        return WishlistResponse.builder()
                .designId(item.getDesignId())
                .title(title)
                .slug(slug)
                .assetUuid(assetUuid)
                .basePriceCents(basePriceCents)
                .finalPriceCents(finalPriceCents)
                .discountPercent(discountPercent)
                .specialOffer(specialOffer)
                .build();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long userId, Long designId) {
        log.info("Removing design {} from wishlist for user {}", designId, userId);
        repository.deleteByUserIdAndDesignId(userId, designId);
    }

    @Override
    @Transactional
    public void removeDesignFromAllWishlists(Long designId) {
        log.info("Removing design {} from all wishlists", designId);
        repository.deleteByDesignId(designId);
    }

    @Override
    public boolean isWishlisted(Long userId, Long designId) {
        return repository.existsByUserIdAndDesignId(userId, designId);
    }
}
