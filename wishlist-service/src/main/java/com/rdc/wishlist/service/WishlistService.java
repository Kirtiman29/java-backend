package com.rdc.wishlist.service;

import com.rdc.wishlist.dto.WishlistResponse;
import java.util.List;

public interface WishlistService {
    void addToWishlist(Long userId, Long designId);
    List<WishlistResponse> getUserWishlist(Long userId);
    void removeFromWishlist(Long userId, Long designId);
    void removeDesignFromAllWishlists(Long designId);
    boolean isWishlisted(Long userId, Long designId);
}
