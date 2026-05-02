package com.rdc.wishlist.repository;

import com.rdc.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    List<Wishlist> findByUserId(Long userId);

    void deleteByUserIdAndDesignId(Long userId, Long designId);

    void deleteByDesignId(Long designId);

    boolean existsByUserIdAndDesignId(Long userId, Long designId);
}
