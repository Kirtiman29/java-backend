package com.rdc.order.coupon.repository;

import com.rdc.order.coupon.entity.CouponCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CouponCodeRepository extends JpaRepository<CouponCode, Long> {
    Optional<CouponCode> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<CouponCode> findAllByOrderByCreatedAtDesc();
    List<CouponCode> findByAutoApplyTrueAndActiveTrueOrderByCreatedAtDesc();
}
