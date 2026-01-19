package com.rdc.admin.service;

import com.rdc.admin.dto.*;
import com.rdc.admin.entity.*;
import com.rdc.admin.repository.DesignRepository;
import com.rdc.admin.util.DesignMapper;
import com.rdc.admin.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DesignService {

    private final DesignRepository repository;
    private final DesignPricingService pricingService;
    private final DesignMapper mapper;
    private final AssetClientService assetClientService;

    // FIX: Added createDesign method
    @Transactional
    public DesignResponse createDesign(DesignCreateRequest request) {
        // 1. Validate asset exists in Asset Service (Port 8090)
        assetClientService.validateAsset(request.getAssetUuid());

        Design design = new Design();
        design.setTitle(request.getTitle());
        design.setSlug(generateUniqueSlug(request.getTitle()));
        design.setDescription(request.getDescription());
        design.setCategoryId(request.getCategoryId());
        design.setBasePriceCents(request.getBasePriceCents());
        design.setAssetUuid(request.getAssetUuid());

        // 2. Handle Segment Enum [cite: 309-310]
        try {
            design.setSegment(Segment.valueOf(request.getSegment().toUpperCase()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid segment value");
        }

        // 3. Automated Pricing & Flags
        design.setDiscountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : 0);
        design.setSpecialOffer(Boolean.TRUE.equals(request.getSpecialOffer()));
        design.setFinalPriceCents(pricingService.calculateFinalPrice(design));

        design.setActive(Boolean.TRUE.equals(request.getActive()));
        design.setDraft(request.getDraft() == null || request.getDraft());

        return mapper.toResponse(repository.save(design));
    }

    // FIX: Added getBySegment method
    @Transactional(readOnly = true)
    public List<DesignResponse> getBySegment(String segment) {
        try {
            Segment seg = Segment.valueOf(segment.toUpperCase());
            // Logic: Active only, never show drafts in navigation [cite: 324-325]
            return repository.findBySegmentAndActiveTrue(seg).stream()
                    .filter(d -> !Boolean.TRUE.equals(d.getDraft()))
                    .map(mapper::toResponse)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid segment: " + segment);
        }
    }

    @Transactional(readOnly = true)
    public List<DesignResponse> getAllDesigns() {
        return repository.findAll().stream().map(mapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DesignResponse getDesignById(Long id) {
        return repository.findById(id).map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Design", id));
    }

    @Transactional
    public DesignResponse updateDesign(Long id, DesignUpdateRequest request) {
        Design design = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Design", id));

        if (request.getTitle() != null) {
            design.setTitle(request.getTitle());
            design.setSlug(generateUniqueSlug(request.getTitle()));
        }
        if (request.getBasePriceCents() != null) {
            design.setBasePriceCents(request.getBasePriceCents().longValue());
            design.setFinalPriceCents(pricingService.calculateFinalPrice(design));
        }

        return mapper.toResponse(repository.save(design));
    }

    @Transactional
    public void deleteDesign(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Design", id);
        }
        repository.deleteById(id);
    }

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