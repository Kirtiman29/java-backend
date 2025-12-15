package com.rdc.admin.service;

import com.rdc.admin.dto.OfferCreateRequest; // <-- NEW IMPORT
import com.rdc.admin.dto.OfferDto;         // <-- NEW IMPORT
import com.rdc.admin.entity.Offer;
import com.rdc.admin.repository.OfferRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OfferService {

    private final OfferRepository offerRepository;

    public OfferService(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    // --- Mapper uses the real OfferDto ---
    private OfferDto mapToDto(Offer entity) {
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

    public OfferDto createOffer(OfferCreateRequest request) {
        // Validation: Ensure discount is percent OR fixed amount, not both
        if (request.getDiscountPercent() != null && request.getDiscountCents() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Offer must specify either discount percent or fixed amount, not both.");
        }

        Offer offer = new Offer();
        offer.setName(request.getName());
        offer.setCode(request.getCode());
        offer.setDesignId(request.getDesignId());
        offer.setCategoryId(request.getCategoryId());
        offer.setDiscountPercent(request.getDiscountPercent());
        offer.setDiscountCents(request.getDiscountCents());
        offer.setStartsAt(request.getStartsAt());
        offer.setEndsAt(request.getEndsAt());
        offer.setActive(request.getActive() != null ? request.getActive() : true);

        return mapToDto(offerRepository.save(offer));
    }

    public List<OfferDto> findAll() {
        return offerRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    // Using OfferCreateRequest for simplicity in updates, too (could create OfferUpdateRequest)
    public OfferDto updateOffer(Long id, OfferCreateRequest request) {
        Offer existing = offerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found."));

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getCode() != null) existing.setCode(request.getCode());
        if (request.getDiscountPercent() != null) existing.setDiscountPercent(request.getDiscountPercent());
        if (request.getDiscountCents() != null) existing.setDiscountCents(request.getDiscountCents());
        if (request.getStartsAt() != null) existing.setStartsAt(request.getStartsAt());
        if (request.getEndsAt() != null) existing.setEndsAt(request.getEndsAt());
        if (request.getActive() != null) existing.setActive(request.getActive());

        // Validation for dual discount can be re-run here too

        return mapToDto(offerRepository.save(existing));
    }

    public void deleteOffer(Long id) {
        if (!offerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found.");
        }
        offerRepository.deleteById(id);
    }
}