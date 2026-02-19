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

    @PostMapping
    public ResponseEntity<Void> addToWishlist(Authentication auth, @RequestBody Map<String, Long> body) {
        Long userId = getUserIdFromAuth(auth);
        Long designId = body.get("designId");
        wishlistService.addToWishlist(userId, designId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<WishlistResponse>> getWishlist(Authentication auth) {
        Long userId = getUserIdFromAuth(auth);
        return ResponseEntity.ok(wishlistService.getUserWishlist(userId));
    }

    @GetMapping("/check/{designId}")
    public ResponseEntity<Boolean> isHearted(Authentication auth, @PathVariable Long designId) {
        Long userId = getUserIdFromAuth(auth);
        return ResponseEntity.ok(wishlistService.isWishlisted(userId, designId));
    }


    @DeleteMapping("/{designId}")
    public ResponseEntity<Void> remove(Authentication auth, @PathVariable Long designId) {
        Long userId = getUserIdFromAuth(auth);
        wishlistService.removeFromWishlist(userId, designId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdFromAuth(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return principal.getUserId();
    }
}