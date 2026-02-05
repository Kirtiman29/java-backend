package com.rdc.order.repository;

import com.rdc.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // ✅ ADD THIS METHOD
    List<OrderItem> findByOrderId(Long orderId);
}
