package com.rdc.wishlist.controller;

import com.rdc.wishlist.dto.WishlistResponse;
import com.rdc.wishlist.security.UserPrincipal;
import com.rdc.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@Slf4j
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

    /**
     * Publicly accessible check to see if a design is wishlisted.
     * PermitAll in SecurityConfig allows this to return 'false' instead of a 401.
     */
    @GetMapping("/check/{designId}")
    public ResponseEntity<Boolean> isHearted(Authentication auth, @PathVariable Long designId) {
        // If the user isn't logged in, they haven't "hearted" anything.
        // Returning 'false' (200 OK) prevents the frontend from redirecting to login.
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
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

    private Long getUserIdFromAuth(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null || !(auth.getPrincipal() instanceof UserPrincipal)) {
            log.warn("Unauthorized access attempt to protected wishlist resource");
            throw new RuntimeException("User not authenticated"); // This triggers a 401 via GlobalExceptionHandler
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        return principal.getUserId();
    }
}