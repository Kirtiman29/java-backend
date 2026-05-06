package com.rdc.order.coupon.controller;

import com.rdc.order.coupon.dto.CouponValidateRequest;
import com.rdc.order.coupon.dto.CouponValidateResponse;
import com.rdc.order.coupon.enums.CouponScope;
import com.rdc.order.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class PublicCouponController {

    private final CouponService couponService;

    @PostMapping("/validate")
    public ResponseEntity<CouponValidateResponse> validateCoupon(
            @Valid @RequestBody CouponValidateRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(couponService.validateCoupon(getUserIdFromJwt(jwt), request));
    }

    @GetMapping("/auto-apply")
    public ResponseEntity<CouponValidateResponse> autoApplyCoupon(
            @RequestParam BigDecimal amount,
            @RequestParam CouponScope scope,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(couponService.autoApplyCoupon(getUserIdFromJwt(jwt), amount, scope));
    }

    private Long getUserIdFromJwt(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user identity.");
        }
    }
}
