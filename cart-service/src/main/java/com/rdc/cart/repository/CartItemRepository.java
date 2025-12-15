package com.rdc.cart.repository;

import com.rdc.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // only active (deleted = false) items
    List<CartItem> findByUserIdAndDeletedFalse(Long userId);
}
