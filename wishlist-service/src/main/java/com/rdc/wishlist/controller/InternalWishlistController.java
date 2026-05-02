package com.rdc.wishlist.controller;

import com.rdc.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/wishlist/internal")
@RequiredArgsConstructor
@Slf4j
public class InternalWishlistController {

    private final WishlistService wishlistService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @DeleteMapping("/design/{designId}")
    public ResponseEntity<Void> removeDesignFromAllWishlists(
            @PathVariable Long designId,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {
        validateKey(key);
        log.info("Internal request: Removing design {} from all wishlists", designId);
        wishlistService.removeDesignFromAllWishlists(designId);
        return ResponseEntity.noContent().build();
    }

    private void validateKey(String key) {
        if (internalServiceKey == null || !internalServiceKey.equals(key)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal service key");
        }
    }
}
