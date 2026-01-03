package com.rdc.wishlist.controller;

import com.rdc.wishlist.dto.WishlistResponse;
import com.rdc.wishlist.security.UserPrincipal;
import com.rdc.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * Add item to wishlist
     * POST /api/wishlist
     */
    @PostMapping
    public ResponseEntity<Void> addToWishlist(Authentication auth, @RequestBody Map<String, Long> body) {
        Long userId = getUserIdFromAuth(auth);
        Long designId = body.get("designId");
        wishlistService.addToWishlist(userId, designId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get user's wishlist
     * GET /api/wishlist
     */
    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getWishlist(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        return ResponseEntity.ok(wishlistService.getUserWishlist(userId));
    }

    /**
     * Check if design is in wishlist (for heart icon state)
     * GET /api/wishlist/check/{designId}
     */
    @GetMapping("/check/{designId}")
    public ResponseEntity<Boolean> isHearted(Authentication auth, @PathVariable Long designId) {
        Long userId = getUserIdFromAuth(auth);
        return ResponseEntity.ok(wishlistService.isWishlisted(userId, designId));
    }

    /**
     * Remove item from wishlist
     * DELETE /api/wishlist/{designId}
     */
    @DeleteMapping("/{designId}")
    public ResponseEntity<Void> remove(Authentication auth, @PathVariable Long designId) {
        Long userId = getUserIdFromAuth(auth);
        wishlistService.removeFromWishlist(userId, designId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extract userId from UserPrincipal in Authentication
     */
    private Long getUserIdFromAuth(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return principal.getUserId();
    }
}