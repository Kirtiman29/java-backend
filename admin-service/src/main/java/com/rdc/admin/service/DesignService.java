package com.rdc.admin.service;

import com.rdc.admin.dto.DesignCreateRequest;
import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.dto.DesignUpdateRequest;
import com.rdc.admin.entity.Design;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.DesignRepository;
import com.rdc.admin.util.DesignMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DesignService {

    private final DesignRepository repository;
    private final DesignPricingService pricingService;
    private final DesignMapper mapper;

    /**
     * Creates a new design with rule-based pricing and default draft status.
     */
    @Transactional
    public DesignResponse createDesign(DesignCreateRequest request) {
        Design design = new Design();
        design.setTitle(request.getTitle());
        design.setSlug(generateUniqueSlug(request.getTitle()));
        design.setDescription(request.getDescription());
        design.setCategoryId(request.getCategoryId());

        // Pricing Logic
        design.setBasePriceCents(request.getBasePriceCents());
        design.setSpecialOffer(request.getSpecialOffer() != null ? request.getSpecialOffer() : false);
        design.setDiscountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : 0);

        // Backend calculates the final price [Rule 7]
        int finalPrice = pricingService.calculateFinalPrice(design);
        design.setFinalPriceCents(finalPrice);

        // Default Workflow States [Rule 4]
        design.setDraft(true);
        design.setActive(false);
        design.setTrending(false);
        design.setEditorsPick(false);
        design.setNewArrival(true); // Default for new items

        design.setTags(request.getTags());
        design.setAssetId(request.getAssetId());
        design.setAssetUuid(request.getAssetUuid());

        return mapper.toResponse(repository.save(design));
    }

    /**
     * Updates an existing design with partial update support and pricing recalculation.
     */
    @Transactional
    public DesignResponse updateDesign(Long id, DesignUpdateRequest request) {
        Design design = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Design not found with id: " + id));

        boolean needsPricingRecalculation = false;

        if (request.getTitle() != null) {
            design.setTitle(request.getTitle());
            design.setSlug(generateUniqueSlug(request.getTitle()));
        }

        if (request.getDescription() != null) design.setDescription(request.getDescription());
        if (request.getCategoryId() != null) design.setCategoryId(request.getCategoryId());

        // Pricing related updates
        if (request.getBasePriceCents() != null) {
            design.setBasePriceCents(request.getBasePriceCents());
            needsPricingRecalculation = true;
        }
        if (request.getSpecialOffer() != null) {
            design.setSpecialOffer(request.getSpecialOffer());
            needsPricingRecalculation = true;
        }
        if (request.getDiscountPercent() != null) {
            design.setDiscountPercent(request.getDiscountPercent());
            needsPricingRecalculation = true;
        }

        if (needsPricingRecalculation) {
            design.setFinalPriceCents(pricingService.calculateFinalPrice(design));
        }

        // Workflow and Section Flags
        if (request.getActive() != null) design.setActive(request.getActive());
        if (request.getDraft() != null) design.setDraft(request.getDraft());
        if (request.getTrending() != null) design.setTrending(request.getTrending());
        if (request.getEditorsPick() != null) design.setEditorsPick(request.getEditorsPick());
        if (request.getNewArrival() != null) design.setNewArrival(request.getNewArrival());

        if (request.getTags() != null) design.setTags(request.getTags());

        return mapper.toResponse(repository.save(design));
    }

    @Transactional(readOnly = true)
    public List<DesignResponse> getAllDesigns() {
        return repository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DesignResponse getDesignById(Long id) {
        Design design = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Design not found with id: " + id));
        return mapper.toResponse(design);
    }

    @Transactional
    public void deleteDesign(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Design not found with id: " + id);
        }
        repository.deleteById(id);
    }

    /**
     * Utility to ensure slugs are URL-friendly and unique.
     */
    private String generateUniqueSlug(String title) {
        String baseSlug = title.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        String slug = baseSlug;
        int count = 1;
        while (repository.existsBySlug(slug)) {
            slug = baseSlug + "-" + count++;
        }
        return slug;
    }
}