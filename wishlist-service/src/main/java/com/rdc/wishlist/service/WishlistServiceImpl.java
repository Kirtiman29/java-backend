package com.rdc.wishlist.service;

import com.rdc.wishlist.client.DesignClientService;
import com.rdc.wishlist.dto.DesignDto;
import com.rdc.wishlist.dto.WishlistResponse;
import com.rdc.wishlist.entity.Wishlist;
import com.rdc.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository repository;
    private final DesignClientService designClientService;

    @Override
    @Transactional
    public void addToWishlist(Long userId, Long designId) {
        log.info("Adding design {} to wishlist for user {}", designId, userId);

        // Validate design exists and is available (not draft, is active)
        designClientService.validateDesignForWishlist(designId);

        // Check if already in wishlist
        if (!repository.existsByUserIdAndDesignId(userId, designId)) {
            Wishlist item = Wishlist.builder()
                    .userId(userId)
                    .designId(designId)
                    .build();
            repository.save(item);
            log.info("Design {} added to wishlist for user {}", designId, userId);
        } else {
            log.info("Design {} already in wishlist for user {}", designId, userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<WishlistResponse> getUserWishlist(Long userId) {
        log.info("Fetching wishlist for user {}", userId);

        List<Wishlist> items = repository.findByUserId(userId);

        return items.stream().map(item -> {
            try {
                // Fetch design from PUBLIC endpoint
                DesignDto design = designClientService.getDesignById(item.getDesignId());

                return WishlistResponse.builder()
                        .designId(item.getDesignId())
                        .title(design.getTitle())
                        .slug(design.getSlug())
                        .assetUuid(design.getAssetUuid())
                        .basePriceCents(design.getBasePriceCents())
                        .finalPriceCents(design.getFinalPriceCents())
                        .discountPercent(design.getDiscountPercent())
                        .specialOffer(design.getSpecialOffer())
                        .build();

            } catch (Exception e) {
                log.warn("Failed to fetch design {}: {}", item.getDesignId(), e.getMessage());
                // Fallback if design service is down
                return WishlistResponse.builder()
                        .designId(item.getDesignId())
                        .build();
            }
        }).collect(Collectors.toList());
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