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
    private final AssetClientService assetClientService;

    @Transactional
    public DesignResponse createDesign(DesignCreateRequest request) {
        // Validate asset exists in Asset-Service before linking [cite: 343]
        assetClientService.validateAsset(request.getAssetUuid());

        Design design = new Design();
        design.setTitle(request.getTitle());
        design.setSlug(generateUniqueSlug(request.getTitle()));
        design.setDescription(request.getDescription());
        design.setCategoryId(request.getCategoryId());
        design.setBasePriceCents(request.getBasePriceCents());

        // Pricing logic
        design.setSpecialOffer(request.getSpecialOffer() != null ? request.getSpecialOffer() : false);
        design.setDiscountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : 0);
        design.setFinalPriceCents(pricingService.calculateFinalPrice(design));

        // FIX: Mapping Status and Section Flags from Request [cite: 193-194]
        design.setActive(request.getActive() != null ? request.getActive() : false);
        design.setDraft(request.getDraft() != null ? request.getDraft() : true);
        design.setTrending(request.getTrending() != null ? request.getTrending() : false);
        design.setEditorsPick(request.getEditorsPick() != null ? request.getEditorsPick() : false);
        design.setNewArrival(request.getNewArrival() != null ? request.getNewArrival() : true);

        design.setTags(request.getTags());
        design.setAssetId(request.getAssetId());
        design.setAssetUuid(request.getAssetUuid());

        return mapper.toResponse(repository.save(design));
    }

    @Transactional
    public DesignResponse updateDesign(Long id, DesignUpdateRequest request) {
        Design design = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Design not found"));

        boolean needsPricingRecalculation = false;

        if (request.getTitle() != null) {
            design.setTitle(request.getTitle());
            design.setSlug(generateUniqueSlug(request.getTitle()));
        }

        if (request.getBasePriceCents() != null) {
            design.setBasePriceCents(request.getBasePriceCents().longValue());
            needsPricingRecalculation = true;
        }

        if (needsPricingRecalculation) {
            design.setFinalPriceCents(pricingService.calculateFinalPrice(design));
        }

        // Mapping flags for updates [cite: 350-351]
        if (request.getActive() != null) design.setActive(request.getActive());
        if (request.getDraft() != null) design.setDraft(request.getDraft());
        if (request.getTrending() != null) design.setTrending(request.getTrending());
        if (request.getEditorsPick() != null) design.setEditorsPick(request.getEditorsPick());
        if (request.getNewArrival() != null) design.setNewArrival(request.getNewArrival());

        return mapper.toResponse(repository.save(design));
    }

    @Transactional
    public void deleteDesign(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Design not found with id: " + id);
        }
        repository.deleteById(id);
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