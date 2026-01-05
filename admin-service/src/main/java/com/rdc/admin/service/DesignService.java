package com.rdc.admin.service;

import com.rdc.admin.dto.DesignCreateRequest;
import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.dto.DesignUpdateRequest;
import com.rdc.admin.entity.Design;
import com.rdc.admin.entity.Segment;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.DesignRepository;
import com.rdc.admin.util.DesignMapper;
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

    @Transactional
    public DesignResponse createDesign(DesignCreateRequest request) {
        // Validate asset exists [cite: 666-671]
        assetClientService.validateAsset(request.getAssetUuid());

        Design design = new Design();
        design.setTitle(request.getTitle());
        design.setSlug(generateUniqueSlug(request.getTitle()));
        design.setDescription(request.getDescription());
        design.setCategoryId(request.getCategoryId());
        design.setBasePriceCents(request.getBasePriceCents());

        // Handle Segment Enum [cite: 611-616]
        try {
            design.setSegment(Segment.valueOf(request.getSegment().toUpperCase()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid segment value. Use MENSWEAR, WOMENSWEAR, KIDSWEAR, or HOME_INTERIOR");
        }

        // Pricing & Flags [cite: 715-716]
        design.setSpecialOffer(request.getSpecialOffer() != null ? request.getSpecialOffer() : false);
        design.setDiscountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : 0);
        design.setFinalPriceCents(pricingService.calculateFinalPrice(design));

        design.setActive(request.getActive() != null ? request.getActive() : false);
        design.setDraft(request.getDraft() != null ? request.getDraft() : true);
        design.setTrending(request.getTrending() != null ? request.getTrending() : false);
        design.setEditorsPick(request.getEditorsPick() != null ? request.getEditorsPick() : false);
        design.setNewArrival(request.getNewArrival() != null ? request.getNewArrival() : true);
        design.setPremium(request.getPremium() != null ? request.getPremium() : false);

        design.setTags(request.getTags());
        design.setAssetId(request.getAssetId());
        design.setAssetUuid(request.getAssetUuid());

        return mapper.toResponse(repository.save(design));
    }

    @Transactional
    public DesignResponse updateDesign(Long id, DesignUpdateRequest request) {
        Design design = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Design not found"));

        if (request.getTitle() != null) {
            design.setTitle(request.getTitle());
            design.setSlug(generateUniqueSlug(request.getTitle()));
        }

        if (request.getSegment() != null) {
            try {
                design.setSegment(Segment.valueOf(request.getSegment().toUpperCase()));
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid segment value");
            }
        }

        if (request.getBasePriceCents() != null) {
            design.setBasePriceCents(request.getBasePriceCents().longValue());
            design.setFinalPriceCents(pricingService.calculateFinalPrice(design));
        }

        // Update Flags [cite: 721-722]
        if (request.getActive() != null) design.setActive(request.getActive());
        if (request.getDraft() != null) design.setDraft(request.getDraft());
        if (request.getTrending() != null) design.setTrending(request.getTrending());
        if (request.getEditorsPick() != null) design.setEditorsPick(request.getEditorsPick());
        if (request.getNewArrival() != null) design.setNewArrival(request.getNewArrival());
        if (request.getPremium() != null) design.setPremium(request.getPremium());

        return mapper.toResponse(repository.save(design));
    }

    @Transactional(readOnly = true)
    public List<DesignResponse> getBySegment(String segment) {
        try {
            Segment seg = Segment.valueOf(segment.toUpperCase());
            return repository.findBySegmentAndActiveTrue(seg)
                    .stream()
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
                .orElseThrow(() -> new ResourceNotFoundException("Design not found"));
    }

    @Transactional
    public void deleteDesign(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Design not found with id: " + id);
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