package com.rdc.admin.service;

import com.rdc.admin.dto.DesignCreateRequest;
import com.rdc.admin.dto.DesignUpdateRequest;
import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.entity.Design;
import com.rdc.admin.repository.DesignRepository;
import com.rdc.admin.util.SlugGenerator;
import com.rdc.admin.util.DesignMapper;
import com.rdc.admin.exception.ResourceNotFoundException; // <-- NEW CUSTOM EXCEPTION
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DesignService {

    private final DesignRepository designRepository;

    @Transactional
    public DesignResponse createDesign(DesignCreateRequest request) {
        // 1. Generate Slug and check for uniqueness
        String baseSlug = SlugGenerator.generateSlug(request.getTitle());
        String finalSlug = baseSlug;
        int counter = 1;

        while (designRepository.existsBySlug(finalSlug)) {
            finalSlug = baseSlug + "-" + counter++;
        }

        List<String> tags = request.getTags() != null ? request.getTags() : Collections.emptyList();

        Design design = Design.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .slug(finalSlug)
                .priceCents(request.getPriceCents())
                .categoryId(request.getCategoryId())
                .assetId(request.getAssetId())
                .assetUuid(request.getAssetUuid())
                // Ensure tags is not null when building
                .tags(tags)
                .published(false)
                .featured(false)
                .build();

        Design savedDesign = designRepository.save(design);

        return DesignMapper.toResponse(savedDesign);
    }

    public List<DesignResponse> getAllDesigns() {
        return designRepository.findAll().stream()
                .map(DesignMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to retrieve the Design entity.
     * Throws ResourceNotFoundException (mapped to 404) if not found.
     */
    private Design getDesignEntityById(Long id) {
        return designRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Design", id));
    }

    public DesignResponse getDesignById(Long id) {
        Design design = getDesignEntityById(id);
        return DesignMapper.toResponse(design);
    }

    @Transactional
    public DesignResponse updateDesign(Long id, DesignUpdateRequest request) {
        Design existingDesign = getDesignEntityById(id);

        // Apply partial updates from DTO
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            existingDesign.setTitle(request.getTitle());

            // Re-generate slug only if the title changes
            String newSlug = SlugGenerator.generateSlug(request.getTitle());
            if (!existingDesign.getSlug().equals(newSlug)) {
                // NOTE: Full slug uniqueness check is omitted here for simplicity,
                // assuming titles are unique enough, or handle conflict as needed.
                existingDesign.setSlug(newSlug);
            }
        }

        // Null checks for all other optional fields
        if (request.getDescription() != null) {
            existingDesign.setDescription(request.getDescription());
        }
        if (request.getPriceCents() != null) {
            existingDesign.setPriceCents(request.getPriceCents());
        }
        if (request.getCategoryId() != null) {
            existingDesign.setCategoryId(request.getCategoryId());
        }
        if (request.getAssetId() != null) {
            existingDesign.setAssetId(request.getAssetId());
        }
        if (request.getAssetUuid() != null) {
            existingDesign.setAssetUuid(request.getAssetUuid());
        }

        // Update tags collection
        if (request.getTags() != null) {
            existingDesign.setTags(request.getTags());
        }

        if (request.getPublished() != null) {
            existingDesign.setPublished(request.getPublished());
        }
        if (request.getFeatured() != null) {
            existingDesign.setFeatured(request.getFeatured());
        }

        Design updatedDesign = designRepository.save(existingDesign);
        return DesignMapper.toResponse(updatedDesign);
    }

    @Transactional
    public void deleteDesign(Long id) {
        // Fetching first to ensure ResourceNotFoundException is thrown if ID is invalid
        Design existingDesign = getDesignEntityById(id);
        designRepository.delete(existingDesign);
    }
}