package com.rdc.order.repository;

import com.rdc.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by ID and user ID.
     * Ensures user can only access their own orders.
     */
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * Find all orders for a user, sorted by creation date descending.
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find orders by status.
     */
    List<Order> findByStatus(String status);

    /**
     * Count orders by user and status.
     */
    long countByUserIdAndStatus(Long userId, String status);
}
