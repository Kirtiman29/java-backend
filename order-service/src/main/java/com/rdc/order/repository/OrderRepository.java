package com.rdc.order.repository;

import com.rdc.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
