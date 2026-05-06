package com.rdc.order.coupon.repository;

import com.rdc.order.coupon.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    long countByCouponIdAndUserId(Long couponId, Long userId);
    boolean existsByCouponIdAndOrderId(Long couponId, Long orderId);
}
