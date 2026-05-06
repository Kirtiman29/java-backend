package com.rdc.order.coupon.service;

import com.rdc.order.client.SubscriptionServiceClient;
import com.rdc.order.coupon.dto.CouponCreateRequest;
import com.rdc.order.coupon.dto.CouponResponse;
import com.rdc.order.coupon.dto.CouponValidateRequest;
import com.rdc.order.coupon.dto.CouponValidateResponse;
import com.rdc.order.coupon.entity.CouponCode;
import com.rdc.order.coupon.entity.CouponUsage;
import com.rdc.order.coupon.enums.AudienceType;
import com.rdc.order.coupon.enums.CouponScope;
import com.rdc.order.coupon.enums.DiscountType;
import com.rdc.order.coupon.repository.CouponCodeRepository;
import com.rdc.order.coupon.repository.CouponUsageRepository;
import com.rdc.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponCodeRepository couponCodeRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionServiceClient subscriptionServiceClient;

    @Transactional
    public CouponResponse createCoupon(CouponCreateRequest request) {
        validateCreateRequest(request);

        String normalizedCode = normalizeCode(request.getCode());
        if (couponCodeRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon code already exists.");
        }

        CouponCode coupon = CouponCode.builder()
                .code(normalizedCode)
                .discountType(request.getDiscountType())
                .discountValue(scaleMoney(request.getDiscountValue()))
                .maxDiscountAmount(scaleNullableMoney(request.getMaxDiscountAmount()))
                .minOrderAmount(scaleNullableMoney(request.getMinOrderAmount()))
                .active(request.getActive() == null || request.getActive())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .perUserLimit(request.getPerUserLimit())
                .audienceType(request.getAudienceType() == null ? AudienceType.ALL : request.getAudienceType())
                .couponScope(request.getCouponScope() == null ? CouponScope.ORDER : request.getCouponScope())
                .autoApply(Boolean.TRUE.equals(request.getAutoApply()))
                .build();

        return toResponse(couponCodeRepository.save(coupon), "Coupon created successfully.");
    }

    @Transactional(readOnly = true)
    public List<CouponResponse> listCoupons() {
        return couponCodeRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(coupon -> toResponse(coupon, null))
                .toList();
    }

    @Transactional
    public CouponResponse updateCouponStatus(Long couponId, boolean active) {
        CouponCode coupon = couponCodeRepository.findById(couponId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Coupon not found."));

        coupon.setActive(active);
        return toResponse(couponCodeRepository.save(coupon), "Coupon status updated successfully.");
    }

    @Transactional(readOnly = true)
    public CouponValidateResponse validateCoupon(Long userId, CouponValidateRequest request) {
        CouponCode coupon = couponCodeRepository.findByCodeIgnoreCase(normalizeCode(request.getCode())).orElse(null);
        ValidationResult result = validateCouponInternal(coupon, userId, request.getAmount(), request.getScope());
        return toValidateResponse(result, null);
    }

    @Transactional(readOnly = true)
    public CouponValidateResponse autoApplyCoupon(Long userId, BigDecimal amount, CouponScope scope) {
        ValidationResult bestMatch = couponCodeRepository.findByAutoApplyTrueAndActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(coupon -> validateCouponInternal(coupon, userId, amount, scope))
                .filter(ValidationResult::valid)
                .max(Comparator.comparing(ValidationResult::discountAmount))
                .orElse(null);

        if (bestMatch == null) {
            return CouponValidateResponse.builder()
                    .valid(false)
                    .available(false)
                    .message("No eligible auto-apply coupon found.")
                    .build();
        }

        return toValidateResponse(bestMatch, true);
    }

    @Transactional(readOnly = true)
    public OrderCouponApplication applyCouponToOrder(Long userId, String couponCode, long orderAmountCents) {
        BigDecimal amount = centsToAmount(orderAmountCents);
        CouponCode coupon = couponCodeRepository.findByCodeIgnoreCase(normalizeCode(couponCode))
                .orElse(null);

        ValidationResult result = validateCouponInternal(coupon, userId, amount, CouponScope.ORDER);
        if (!result.valid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, result.message());
        }

        return new OrderCouponApplication(
                result.coupon().getCode(),
                amountToCents(result.discountAmount()),
                amountToCents(result.finalAmount())
        );
    }

    @Transactional
    public void recordCouponUsage(String couponCode, Long userId, Long orderId, long discountAmountCents) {
        if (couponCode == null || couponCode.isBlank() || discountAmountCents <= 0) {
            return;
        }

        couponCodeRepository.findByCodeIgnoreCase(normalizeCode(couponCode)).ifPresentOrElse(coupon -> {
            if (couponUsageRepository.existsByCouponIdAndOrderId(coupon.getId(), orderId)) {
                return;
            }

            couponUsageRepository.save(CouponUsage.builder()
                    .coupon(coupon)
                    .userId(userId)
                    .orderId(orderId)
                    .discountAmount(centsToAmount(discountAmountCents))
                    .usedAt(LocalDateTime.now())
                    .build());

            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponCodeRepository.save(coupon);
        }, () -> log.warn("Coupon {} was applied on order {} but no longer exists for usage recording", couponCode, orderId));
    }

    private ValidationResult validateCouponInternal(CouponCode coupon, Long userId, BigDecimal amount, CouponScope scope) {
        BigDecimal normalizedAmount = scaleMoney(amount);
        if (coupon == null) {
            return ValidationResult.invalid("Coupon not found.");
        }
        if (!coupon.isActive()) {
            return ValidationResult.invalid("Coupon is inactive.");
        }
        if (!scopeMatches(coupon.getCouponScope(), scope)) {
            return ValidationResult.invalid("Coupon is not valid for this scope.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartDate() != null && now.isBefore(coupon.getStartDate())) {
            return ValidationResult.invalid("Coupon is not active yet.");
        }
        if (coupon.getEndDate() != null && now.isAfter(coupon.getEndDate())) {
            return ValidationResult.invalid("Coupon expired.");
        }
        if (coupon.getMinOrderAmount() != null && normalizedAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            return ValidationResult.invalid("Minimum order amount not reached.");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            return ValidationResult.invalid("Coupon usage limit exceeded.");
        }
        if (coupon.getPerUserLimit() != null) {
            long userUsageCount = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId);
            if (userUsageCount >= coupon.getPerUserLimit()) {
                return ValidationResult.invalid("Coupon per-user usage limit exceeded.");
            }
        }
        if (!isAudienceEligible(coupon.getAudienceType(), userId)) {
            return ValidationResult.invalid(audienceFailureMessage(coupon.getAudienceType()));
        }

        BigDecimal discountAmount = calculateDiscount(coupon, normalizedAmount);
        BigDecimal finalAmount = scaleMoney(normalizedAmount.subtract(discountAmount).max(BigDecimal.ZERO));

        return ValidationResult.valid(coupon, discountAmount, finalAmount, "Coupon applied successfully.");
    }

    private boolean isAudienceEligible(AudienceType audienceType, Long userId) {
        long paidOrderCount = orderRepository.countPaidOrdersByUserId(userId);

        return switch (audienceType) {
            case ALL -> true;
            case NEW_USER -> paidOrderCount == 0;
            case EXISTING_USER -> paidOrderCount > 0;
            case SUBSCRIBER -> subscriptionServiceClient.hasActiveSubscription(userId);
        };
    }

    private String audienceFailureMessage(AudienceType audienceType) {
        return switch (audienceType) {
            case ALL -> "Coupon is not applicable.";
            case NEW_USER -> "This coupon is only available for new users.";
            case EXISTING_USER -> "This coupon is only available for existing users.";
            case SUBSCRIBER -> "This coupon is only available for subscribers.";
        };
    }

    private boolean scopeMatches(CouponScope couponScope, CouponScope requestScope) {
        return couponScope == CouponScope.ALL || couponScope == requestScope;
    }

    private BigDecimal calculateDiscount(CouponCode coupon, BigDecimal amount) {
        BigDecimal discountAmount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = amount
                    .multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (coupon.getMaxDiscountAmount() != null) {
                discountAmount = discountAmount.min(coupon.getMaxDiscountAmount());
            }
        } else {
            discountAmount = coupon.getDiscountValue();
        }

        return scaleMoney(discountAmount.min(amount).max(BigDecimal.ZERO));
    }

    private void validateCreateRequest(CouponCreateRequest request) {
        if (request.getEndDate() != null && request.getStartDate() != null
                && request.getEndDate().isBefore(request.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be after start date.");
        }
        if (request.getUsageLimit() != null && request.getUsageLimit() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usage limit must be at least 1.");
        }
        if (request.getPerUserLimit() != null && request.getPerUserLimit() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Per-user limit must be at least 1.");
        }
        if (request.getDiscountType() == DiscountType.PERCENTAGE
                && request.getDiscountValue() != null
                && request.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Percentage discount cannot exceed 100.");
        }
    }

    private CouponResponse toResponse(CouponCode coupon, String message) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .minOrderAmount(coupon.getMinOrderAmount())
                .active(coupon.isActive())
                .startDate(coupon.getStartDate())
                .endDate(coupon.getEndDate())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .perUserLimit(coupon.getPerUserLimit())
                .audienceType(coupon.getAudienceType())
                .couponScope(coupon.getCouponScope())
                .autoApply(coupon.isAutoApply())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .message(message)
                .build();
    }

    private CouponValidateResponse toValidateResponse(ValidationResult result, Boolean available) {
        return CouponValidateResponse.builder()
                .valid(result.valid())
                .available(available)
                .couponCode(result.coupon() != null ? result.coupon().getCode() : null)
                .discountType(result.coupon() != null ? result.coupon().getDiscountType() : null)
                .discountValue(result.coupon() != null ? result.coupon().getDiscountValue() : null)
                .discountAmount(result.discountAmount())
                .finalAmount(result.finalAmount())
                .message(result.message())
                .build();
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal scaleMoney(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleNullableMoney(BigDecimal amount) {
        return amount == null ? null : scaleMoney(amount);
    }

    private BigDecimal centsToAmount(long amountCents) {
        return BigDecimal.valueOf(amountCents, 2).setScale(2, RoundingMode.HALF_UP);
    }

    private long amountToCents(BigDecimal amount) {
        return amount
                .movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private record ValidationResult(
            boolean valid,
            CouponCode coupon,
            BigDecimal discountAmount,
            BigDecimal finalAmount,
            String message
    ) {
        private static ValidationResult valid(CouponCode coupon, BigDecimal discountAmount, BigDecimal finalAmount, String message) {
            return new ValidationResult(true, coupon, discountAmount, finalAmount, message);
        }

        private static ValidationResult invalid(String message) {
            return new ValidationResult(false, null, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), null, message);
        }
    }

    public record OrderCouponApplication(
            String couponCode,
            long discountAmountCents,
            long finalAmountCents
    ) {
    }
}
