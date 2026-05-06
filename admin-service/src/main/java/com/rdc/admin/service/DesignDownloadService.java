package com.rdc.admin.service;

import com.rdc.admin.client.OrderServiceClient;
import com.rdc.admin.client.SubscriptionServiceClient;
import com.rdc.admin.dto.DesignDownloadRequestResponse;
import com.rdc.admin.dto.DesignDownloadResponse;
import com.rdc.admin.dto.order.SubscriptionDownloadOrderRequest;
import com.rdc.admin.dto.order.SubscriptionDownloadOrderResponse;
import com.rdc.admin.dto.subscription.SubscriptionDesignValidationResponse;
import com.rdc.admin.dto.subscription.SubscriptionSummaryResponse;
import com.rdc.admin.entity.Design;
import com.rdc.admin.entity.DesignDownloadRequest;
import com.rdc.admin.entity.DesignDownloadStatus;
import com.rdc.admin.repository.DesignDownloadRequestRepository;
import com.rdc.admin.repository.DesignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignDownloadService {

    private final DesignRepository designRepository;
    private final DesignDownloadRequestRepository designDownloadRequestRepository;
    private final SubscriptionServiceClient subscriptionServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final DesignService designService;
    private final RestTemplate restTemplate;

    @Value("${service.cart.url}")
    private String cartServiceUrl;

    @Value("${service.wishlist.url}")
    private String wishlistServiceUrl;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    @Transactional
    public DesignDownloadResponse requestDownload(Long userId, Long designId) {
        Design design = designRepository.findByIdForUpdate(designId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Design not found"));

        if (!Boolean.TRUE.equals(design.getActive()) || Boolean.TRUE.equals(design.getDraft())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Design not available");
        }

        if (!Boolean.TRUE.equals(design.getSubscriptionOnly())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This design is not eligible for subscription download");
        }

        DesignDownloadRequest existingForDesign = designDownloadRequestRepository
                .findFirstByDesignIdOrderByCreatedAtDesc(designId)
                .orElse(null);

        if (existingForDesign != null) {
            if (existingForDesign.getUserId().equals(userId)) {
                Integer remaining = getRemainingDesigns(userId);
                return mapResponse(existingForDesign, remaining, true, "Download request already exists for this design");
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This design has already been purchased and removed from the subscription inventory"
            );
        }

        // Reserve the design for the first requester while this transaction is in progress.
        design.setActive(false);
        design.setDraft(true);
        designRepository.saveAndFlush(design);

        SubscriptionDesignValidationResponse validation = subscriptionServiceClient.validateDesign(userId);
        if (validation == null || !validation.isAllowed()) {
            throw new ResponseStatusException(resolveValidationStatus(validation), validationMessage(validation));
        }

        SubscriptionDesignValidationResponse consumption = subscriptionServiceClient.consumeDesign(userId);
        if (consumption == null || !consumption.isAllowed()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, validationMessage(consumption));
        }

        SubscriptionDownloadOrderResponse order = orderServiceClient.createSubscriptionDownloadOrder(
                SubscriptionDownloadOrderRequest.builder()
                        .userId(userId)
                        .designId(design.getId())
                        .designIdentifier(design.getDesignIdentifier())
                        .designTitle(design.getTitle())
                        .assetUuid(design.getAssetUuid())
                        .remainingDesigns(consumption.getRemainingDesigns())
                        .build()
        );

        if (order == null || order.getOrderId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to record subscription download order");
        }

        DesignDownloadRequest request = designDownloadRequestRepository.save(
                DesignDownloadRequest.builder()
                        .userId(userId)
                        .designId(design.getId())
                        .designIdentifier(design.getDesignIdentifier())
                        .designTitle(design.getTitle())
                        .orderId(order.getOrderId())
                        .status(DesignDownloadStatus.PENDING)
                        .build()
        );

        designService.purgeDesignAndRecord(design.getId(), order.getOrderId());
        removeDesignFromAllCarts(design.getId());
        removeDesignFromAllWishlists(design.getId());

        return mapResponse(request, consumption.getRemainingDesigns(), false,
                "Design download recorded successfully. TIFF will be shared manually.");
    }

    @Transactional(readOnly = true)
    public List<DesignDownloadRequestResponse> getAllRequests() {
        return designDownloadRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapRequest)
                .toList();
    }

    @Transactional
    public DesignDownloadRequestResponse markAsSent(Long requestId) {
        DesignDownloadRequest request = designDownloadRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Download request not found"));

        request.setStatus(DesignDownloadStatus.SENT);
        request.setSentAt(LocalDateTime.now());

        return mapRequest(designDownloadRequestRepository.save(request));
    }

    private Integer getRemainingDesigns(Long userId) {
        SubscriptionSummaryResponse summary = subscriptionServiceClient.getSubscriptionSummary(userId);
        return summary != null ? summary.getRemainingDesigns() : null;
    }

    private HttpStatus resolveValidationStatus(SubscriptionDesignValidationResponse validation) {
        if (validation != null && validation.getMessage() != null
                && validation.getMessage().toLowerCase().contains("exhausted")) {
            return HttpStatus.CONFLICT;
        }

        return HttpStatus.FORBIDDEN;
    }

    private String validationMessage(SubscriptionDesignValidationResponse validation) {
        return validation != null && validation.getMessage() != null
                ? validation.getMessage()
                : "Subscription validation failed";
    }

    private void removeDesignFromAllCarts(Long designId) {
        try {
            String url = cartServiceUrl + "/api/cart/internal/design/" + designId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception ex) {
            log.warn("Failed to clear design {} from carts after subscription allocation: {}", designId, ex.getMessage());
        }
    }

    private void removeDesignFromAllWishlists(Long designId) {
        try {
            String url = wishlistServiceUrl + "/api/wishlist/internal/design/" + designId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-INTERNAL-KEY", internalServiceKey);
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers), Void.class);
        } catch (Exception ex) {
            log.warn("Failed to clear design {} from wishlists after subscription allocation: {}", designId, ex.getMessage());
        }
    }

    private DesignDownloadResponse mapResponse(
            DesignDownloadRequest request,
            Integer remainingDesigns,
            boolean alreadyRequested,
            String message
    ) {
        return DesignDownloadResponse.builder()
                .requestId(request.getId())
                .orderId(request.getOrderId())
                .designId(request.getDesignId())
                .designIdentifier(request.getDesignIdentifier())
                .designTitle(request.getDesignTitle())
                .status(request.getStatus())
                .remainingDesigns(remainingDesigns)
                .alreadyRequested(alreadyRequested)
                .message(message)
                .build();
    }

    private DesignDownloadRequestResponse mapRequest(DesignDownloadRequest request) {
        return DesignDownloadRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .designId(request.getDesignId())
                .designIdentifier(request.getDesignIdentifier())
                .designTitle(request.getDesignTitle())
                .orderId(request.getOrderId())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .sentAt(request.getSentAt())
                .build();
    }
}
