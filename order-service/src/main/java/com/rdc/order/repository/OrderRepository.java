package com.rdc.order.repository;

import com.rdc.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {


    List<Order> findByUserIdAndStatus(Long userId, String status);


    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(o)
            FROM Order o
            WHERE o.userId = :userId
              AND o.status = 'PAID'
              AND (o.purchaseType IS NULL OR UPPER(o.purchaseType) <> 'SUBSCRIPTION_DOWNLOAD')
            """)
    long countPaidOrdersByUserId(@Param("userId") Long userId);

    Optional<Order> findByIdAndUserId(Long id, Long userId);
}
