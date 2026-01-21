package com.rdc.admin.service;

import com.rdc.admin.dto.*;
import com.rdc.admin.entity.*;
import com.rdc.admin.repository.DesignRepository;
import com.rdc.admin.repository.DesignMediaRepository;
import com.rdc.admin.util.DesignMapper;
import com.rdc.admin.util.SlugGenerator;
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
    private final DesignMediaRepository mediaRepository;
    private final DesignMapper mapper;
    private final DesignPricingService pricingService;
    private final AssetClientService assetClientService;

    @Transactional(readOnly = true)
    public List<DesignResponse> getAllDesigns() {
        return repository.findByDraftFalseAndActiveTrue().stream()
                .map(design -> mapper.toResponse(design, mediaRepository.findByDesignId(design.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DesignResponse getDesignBySlug(String slug) {
        Design design = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Design not found with slug: " + slug));
        return mapper.toResponse(design, mediaRepository.findByDesignId(design.getId()));
    }

    @Transactional
    public DesignResponse createDesign(DesignCreateRequest request) {
        Design design = new Design();
        design.setTitle(request.getTitle());
        design.setSlug(generateUniqueSlug(request.getTitle()));
        design.setDescription(request.getDescription());
        design.setBasePriceCents(request.getBasePriceCents());

        // ✅ Ensure categoryId is explicitly set
        design.setCategoryId(request.getCategoryId());

        if (request.getSegment() != null) {
            design.setSegment(Segment.valueOf(request.getSegment().toUpperCase()));
        }

        // ✅ Set All Flags (Prevent Nulls)
        design.setActive(Boolean.TRUE.equals(request.getActive()));
        design.setDraft(Boolean.TRUE.equals(request.getDraft()));
        design.setTrending(Boolean.TRUE.equals(request.getTrending()));
        design.setEditorsPick(Boolean.TRUE.equals(request.getEditorsPick()));
        design.setNewArrival(Boolean.TRUE.equals(request.getNewArrival()));
        design.setPremium(Boolean.TRUE.equals(request.getPremium()));

        // ✅ Pricing Logic (Prevent Nulls)
        design.setDiscountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : 0);
        design.setSpecialOffer(Boolean.TRUE.equals(request.getSpecialOffer()));
        design.setFinalPriceCents(pricingService.calculateFinalPrice(design));

        Design savedDesign = repository.save(design);

        // Process Media
        saveMedia(savedDesign.getId(), request.getCoverAssetUuid(), AssetType.IMAGE, MediaRole.COVER, 0);
        if (request.getGalleryUuids() != null) {
            for (int i = 0; i < request.getGalleryUuids().size(); i++) {
                saveMedia(savedDesign.getId(), request.getGalleryUuids().get(i), AssetType.IMAGE, MediaRole.GALLERY, i + 1);
            }
        }
        saveMedia(savedDesign.getId(), request.getPreviewVideoUuid(), AssetType.VIDEO, MediaRole.PREVIEW_VIDEO, 0);
        saveMedia(savedDesign.getId(), request.getDownloadTiffUuid(), AssetType.TIFF, MediaRole.DOWNLOAD, 0);

        // Refresh and map
        return mapper.toResponse(savedDesign, mediaRepository.findByDesignId(savedDesign.getId()));
    }

    @Transactional
    public DesignResponse updateDesign(Long id, DesignUpdateRequest request) {
        Design design = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Design", id));

        if (request.getTitle() != null) {
            design.setTitle(request.getTitle());
            design.setSlug(generateUniqueSlug(request.getTitle()));
        }
        if (request.getDescription() != null) design.setDescription(request.getDescription());
        if (request.getCategoryId() != null) design.setCategoryId(request.getCategoryId());
        if (request.getSegment() != null) design.setSegment(Segment.valueOf(request.getSegment().toUpperCase()));

        if (request.getBasePriceCents() != null) design.setBasePriceCents(request.getBasePriceCents());
        if (request.getDiscountPercent() != null) design.setDiscountPercent(request.getDiscountPercent());
        if (request.getSpecialOffer() != null) design.setSpecialOffer(request.getSpecialOffer());

        design.setFinalPriceCents(pricingService.calculateFinalPrice(design));

        // Update Flags
        if (request.getActive() != null) design.setActive(request.getActive());
        if (request.getDraft() != null) design.setDraft(request.getDraft());
        if (request.getTrending() != null) design.setTrending(request.getTrending());
        if (request.getEditorsPick() != null) design.setEditorsPick(request.getEditorsPick());
        if (request.getNewArrival() != null) design.setNewArrival(request.getNewArrival());
        if (request.getPremium() != null) design.setPremium(request.getPremium());

        updateDesignMedia(design.getId(), request);

        return mapper.toResponse(repository.save(design), mediaRepository.findByDesignId(id));
    }

    private void updateDesignMedia(Long designId, DesignUpdateRequest request) {
        if (request.getCoverAssetUuid() != null) replaceMediaRole(designId, MediaRole.COVER, List.of(request.getCoverAssetUuid()), AssetType.IMAGE);
        if (request.getGalleryUuids() != null) replaceMediaRole(designId, MediaRole.GALLERY, request.getGalleryUuids(), AssetType.IMAGE);
        if (request.getPreviewVideoUuid() != null) replaceMediaRole(designId, MediaRole.PREVIEW_VIDEO, List.of(request.getPreviewVideoUuid()), AssetType.VIDEO);
        if (request.getDownloadTiffUuid() != null) replaceMediaRole(designId, MediaRole.DOWNLOAD, List.of(request.getDownloadTiffUuid()), AssetType.TIFF);
    }

    private void replaceMediaRole(Long designId, MediaRole role, List<String> uuids, AssetType type) {
        List<DesignMedia> existing = mediaRepository.findByDesignId(designId).stream()
                .filter(m -> m.getMediaRole() == role).collect(Collectors.toList());
        mediaRepository.deleteAll(existing);
        for (int i = 0; i < uuids.size(); i++) {
            saveMedia(designId, uuids.get(i), type, role, i);
        }
    }

    private void saveMedia(Long designId, String uuid, AssetType type, MediaRole role, int order) {
        if (uuid == null || uuid.isBlank()) return;
        assetClientService.validateAsset(uuid);
        mediaRepository.save(DesignMedia.builder().designId(designId).assetUuid(uuid).assetType(type).mediaRole(role).sortOrder(order).build());
    }

    @Transactional(readOnly = true)
    public List<DesignResponse> getBySegment(String segment) {
        try {
            Segment seg = Segment.valueOf(segment.toUpperCase());
            return repository.findBySegmentAndActiveTrue(seg).stream()
                    .filter(d -> !Boolean.TRUE.equals(d.getDraft()))
                    .map(d -> mapper.toResponse(d, mediaRepository.findByDesignId(d.getId())))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid segment");
        }
    }

    @Transactional(readOnly = true)
    public DesignResponse getDesignById(Long id) {
        Design design = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Design", id));
        return mapper.toResponse(design, mediaRepository.findByDesignId(design.getId()));
    }

    @Transactional
    public void deleteDesign(Long id) {
        if (!repository.existsById(id)) throw new ResourceNotFoundException("Design", id);
        repository.deleteById(id);
    }

    private String generateUniqueSlug(String title) {
        String baseSlug = SlugGenerator.generateSlug(title);
        String slug = baseSlug;
        int count = 1;
        while (repository.existsBySlug(slug)) {
            slug = baseSlug + "-" + count++;
        }
        return slug;
    }
}