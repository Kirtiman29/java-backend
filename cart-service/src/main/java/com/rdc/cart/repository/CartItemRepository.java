package com.rdc.cart.repository;

import com.rdc.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Find all active (non-deleted) cart items for a user[cite: 285].
     */
    List<CartItem> findByUserIdAndDeletedFalse(Long userId);

    /**
     * Find a specific cart item by ID and user ID for security[cite: 286].
     */
    Optional<CartItem> findByIdAndUserIdAndDeletedFalse(Long id, Long userId);

    /**
     * Check if a design already exists in user's cart to update quantity[cite: 288].
     */
    Optional<CartItem> findByUserIdAndDesignIdAndDeletedFalse(Long userId, Long designId);

    /**
     * Soft delete all cart items for a user after order creation.
     * Marks items as deleted without removing them from the database audit trail.
     */
    @Modifying
    @Query("UPDATE CartItem c SET c.deleted = true, c.updatedAt = CURRENT_TIMESTAMP WHERE c.userId = :userId AND c.deleted = false")
    int softDeleteAllByUserId(@Param("userId") Long userId);

    /**
     * Count active items in user's cart[cite: 291].
     */
    long countByUserIdAndDeletedFalse(Long userId);
}