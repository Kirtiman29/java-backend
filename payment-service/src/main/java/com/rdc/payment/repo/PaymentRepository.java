package com.rdc.payment.repo;

import com.rdc.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    // This supports the getPaymentsByUser call in the Service [cite: 380, 388]
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
}