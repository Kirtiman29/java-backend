package com.rdc.order.coupon.controller;

import com.rdc.order.coupon.dto.CouponCreateRequest;
import com.rdc.order.coupon.dto.CouponResponse;
import com.rdc.order.coupon.dto.CouponStatusUpdateRequest;
import com.rdc.order.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(
            @Valid @RequestBody CouponCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        ensureAdmin(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @GetMapping
    public ResponseEntity<List<CouponResponse>> listCoupons(@AuthenticationPrincipal Jwt jwt) {
        ensureAdmin(jwt);
        return ResponseEntity.ok(couponService.listCoupons());
    }

    @PatchMapping("/{couponId}/status")
    public ResponseEntity<CouponResponse> updateCouponStatus(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponStatusUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        ensureAdmin(jwt);
        return ResponseEntity.ok(couponService.updateCouponStatus(couponId, request.getActive()));
    }

    private void ensureAdmin(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("role");
        if (roles == null || !roles.contains("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required.");
        }
    }
}
