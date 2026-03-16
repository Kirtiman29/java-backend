package com.rdc.wishlist.controller;

import com.rdc.wishlist.dto.WishlistResponse;
import com.rdc.wishlist.security.UserPrincipal;
import com.rdc.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ResponseEntity<Void> addToWishlist(Authentication auth, @RequestBody Map<String, Long> body) {

        Long userId = getUserIdFromAuth(auth);

        Long designId = body.get("designId");
        if (designId == null) {
            return ResponseEntity.badRequest().build();
        }

        wishlistService.addToWishlist(userId, designId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getWishlist(Authentication auth) {

        Long userId = getUserIdFromAuth(auth);

        List<WishlistResponse> wishlist = wishlistService.getUserWishlist(userId);
        return ResponseEntity.ok(wishlist);
    }

    @GetMapping("/check/{designId}")
    public ResponseEntity<Boolean> isHearted(Authentication auth, @PathVariable Long designId) {

        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            return ResponseEntity.ok(false);
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();

        boolean result = wishlistService.isWishlisted(principal.getUserId(), designId);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{designId}")
    public ResponseEntity<Void> remove(Authentication auth, @PathVariable Long designId) {

        Long userId = getUserIdFromAuth(auth);

        wishlistService.removeFromWishlist(userId, designId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Safely extract userId from authentication
     */
    private Long getUserIdFromAuth(Authentication auth) {

        if (auth == null || auth.getPrincipal() == null) {
            throw new RuntimeException("User not authenticated");
        }

        if (!(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new RuntimeException("Invalid authentication principal");
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return principal.getUserId();
    }
}