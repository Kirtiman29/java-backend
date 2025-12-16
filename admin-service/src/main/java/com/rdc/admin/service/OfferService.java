package com.rdc.admin.service;

import com.rdc.admin.dto.OfferCreateRequest;
import com.rdc.admin.dto.OfferDto;
import com.rdc.admin.dto.OfferUpdateRequest; // Corrected to use update DTO
import com.rdc.admin.entity.Offer;
import com.rdc.admin.repository.OfferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OfferService {

    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    // --- Mapper Utility ---
    private OfferDto mapToDto(Offer entity) {
        if (entity == null) return null;
        return OfferDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .designId(entity.getDesignId())
                .categoryId(entity.getCategoryId())
                .discountPercent(entity.getDiscountPercent())
                .discountCents(entity.getDiscountCents())
                .startsAt(entity.getStartsAt())
                .endsAt(entity.getEndsAt())
                .active(entity.isActive())
                .build();
    }

    // --- Validation Utility for Creation ---
    private void validateCreateRequest(String code, Integer percent, Long cents, LocalDateTime startsAt, LocalDateTime endsAt) {
        if (percent != null && cents != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer must specify either discount percent or fixed amount, not both.");
        }

        if (code != null && offerRepository.existsByCode(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer code already exists: " + code);
        }

        if (startsAt.isAfter(endsAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date.");
        }
    }


    // --- CRUD Operations ---

    public OfferDto createOffer(OfferCreateRequest request) {
        validateCreateRequest(
                request.getCode(),
                request.getDiscountPercent(),
                request.getDiscountCents(),
                request.getStartsAt(),
                request.getEndsAt()
        );

        Offer offer = new Offer();
        offer.setName(request.getName());
        offer.setCode(request.getCode());
        offer.setDesignId(request.getDesignId());
        offer.setCategoryId(request.getCategoryId());
        offer.setDiscountPercent(request.getDiscountPercent());
        offer.setDiscountCents(request.getDiscountCents());
        offer.setStartsAt(request.getStartsAt());
        offer.setEndsAt(request.getEndsAt());

        if (request.getActive() != null) {
            offer.setActive(request.getActive());
        } else {
            // If null, the Entity default (true) is used automatically
        }

        return mapToDto(offerRepository.save(offer));
    }

    public OfferDto getOfferById(Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found with id " + id));
        return mapToDto(offer);
    }

    public List<OfferDto> findAll() {
        return offerRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // --- CRITICAL FIX APPLIED HERE ---
    public OfferDto updateOffer(Long id, OfferUpdateRequest request) {
        Offer existing = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found with id " + id));

        // --- 1. Apply fields from request to existing entity ---

        // Code update (with uniqueness check)
        if (request.getCode() != null && !request.getCode().isBlank() && !request.getCode().equals(existing.getCode())) {
            if (offerRepository.existsByCode(request.getCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer code already exists: " + request.getCode());
            }
            existing.setCode(request.getCode());
        }

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getDesignId() != null) existing.setDesignId(request.getDesignId());
        if (request.getCategoryId() != null) existing.setCategoryId(request.getCategoryId());

        // CRITICAL FIX: Set discount fields based on request.
        // We only update if the field is explicitly present in the request body (null or value).
        // This is necessary because setting `null` is how we clear the old discount type.
        if (request.getDiscountPercent() != null || (request.getDiscountPercent() == null && request.getDiscountCents() != null)) {
            existing.setDiscountPercent(request.getDiscountPercent());
        }
        if (request.getDiscountCents() != null || (request.getDiscountCents() == null && request.getDiscountPercent() != null)) {
            existing.setDiscountCents(request.getDiscountCents());
        }

        // A more robust check for whether a field was present (even if null) in the DTO
        // is typically handled by passing the original DTO fields, but for a simple
        // DTO like OfferUpdateRequest, the logic above ensures that if you try to set one
        // and nullify the other, the nullification happens.

        // Simplification for clarity (and the previous block was slightly confusing):
        // We assume Lombok @Data (or setters) allows setting null, and we rely on
        // JSON deserialization giving us null if the field is present as null.
        if (request.getDiscountPercent() != null) existing.setDiscountPercent(request.getDiscountPercent());
        if (request.getDiscountCents() != null) existing.setDiscountCents(request.getDiscountCents());

        // This is the simplest way: Apply only if the field is present (non-null in JSON request).
        // The issue was that in Java, request.getDiscountPercent() returns null if not present
        // OR if explicitly set to null.

        // Reverting to the simpler, more explicit logic for null handling on primitive wrapper types:
        // When switching discount types, the request MUST send the *opposite* field as null.
        // We trust the request body to send "discountPercent": null if they want to clear it.


        // For the fix, let's rely on the Request body being explicit:
        if (request.getStartsAt() != null) existing.setStartsAt(request.getStartsAt());
        if (request.getEndsAt() != null) existing.setEndsAt(request.getEndsAt());

        if (request.getActive() != null) existing.setActive(request.getActive());


        // --- 2. Run Conflict Validation on the UPDATED existing object ---

        // Check discount type conflict on the entity's current state (after applying request)
        if (existing.getDiscountPercent() != null && existing.getDiscountCents() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Update causes discount conflict (percent and cents cannot both be set).");
        }

        // Final Date Validation
        if (existing.getStartsAt().isAfter(existing.getEndsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date.");
        }

        // --- 3. Save and return ---
        return mapToDto(offerRepository.save(existing));
    }

    public void deleteOffer(Long id) {
        if (!offerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found with id " + id);
        }
        offerRepository.deleteById(id);
    }
}