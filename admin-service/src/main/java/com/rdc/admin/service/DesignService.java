package com.rdc.admin.service;

import com.rdc.admin.dto.*;
import com.rdc.admin.entity.*;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.DesignDeletionRecordRepository;
import com.rdc.admin.repository.DesignMediaRepository;
import com.rdc.admin.repository.DesignRepository;
import com.rdc.admin.util.DesignMapper;
import com.rdc.admin.util.SlugGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignService {

    private final DesignRepository repository;
    private final DesignMediaRepository mediaRepository;
    private final DesignDeletionRecordRepository deletionRecordRepository;
    private final DesignMapper mapper;
    private final DesignPricingService pricingService;
    private final AssetClientService assetClientService;

    /* ================= READ ================= */

    @Transactional(readOnly = true)
    public List<DesignResponse> getAllDesigns() {
        return repository.findByDraftFalseAndActiveTrue()
                .stream()
                .map(d -> mapper.toResponse(d, mediaRepository.findByDesignId(d.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DesignResponse getDesignBySlug(String slug) {
        Design design = repository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Design not found with slug: " + slug));
        return mapper.toResponse(design, mediaRepository.findByDesignId(design.getId()));
    }

    @Transactional(readOnly = true)
    public DesignResponse getDesignById(Long id) {
        Design design = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Design", id));
        return mapper.toResponse(design, mediaRepository.findByDesignId(design.getId()));
    }

    @Transactional(readOnly = true)
    public List<DesignResponse> getBySegment(String segment) {
        try {
            Segment seg = Segment.valueOf(segment.toUpperCase());
            return repository.findBySegmentAndActiveTrue(seg)
                    .stream()
                    .filter(d -> !Boolean.TRUE.equals(d.getDraft()))
                    .map(d -> mapper.toResponse(d, mediaRepository.findByDesignId(d.getId())))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid segment");
        }
    }

    /* ================= CREATE ================= */

    @Transactional
    public DesignResponse createDesign(DesignCreateRequest request) {
        if (request.getDesignIdentifier() == null || request.getDesignIdentifier().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "designIdentifier is required");
        }
        if (repository.existsByDesignIdentifier(request.getDesignIdentifier())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "designIdentifier already exists");
        }

        Design design = new Design();
        design.setTitle(request.getTitle());
        design.setSlug(generateUniqueSlug(request.getTitle()));
        design.setDescription(request.getDescription());
        design.setDesignIdentifier(request.getDesignIdentifier());
        design.setBasePriceCents(request.getBasePriceCents());
        design.setCategoryId(request.getCategoryId());
        design.setAssetUuid(request.getCoverAssetUuid());

        if (request.getTags() != null) {
            design.getTags().clear();
            design.getTags().addAll(request.getTags());
        }

        if (request.getSegment() != null) {
            design.setSegment(Segment.valueOf(request.getSegment().toUpperCase()));
        }

        design.setActive(Boolean.TRUE.equals(request.getActive()));
        design.setDraft(Boolean.TRUE.equals(request.getDraft()));
        design.setTrending(Boolean.TRUE.equals(request.getTrending()));
        design.setEditorsPick(Boolean.TRUE.equals(request.getEditorsPick()));
        design.setNewArrival(Boolean.TRUE.equals(request.getNewArrival()));
        design.setPremium(Boolean.TRUE.equals(request.getPremium()));
        design.setDiscountPercent(request.getDiscountPercent() != null ? request.getDiscountPercent() : 0);
        design.setSpecialOffer(Boolean.TRUE.equals(request.getSpecialOffer()));
        design.setFinalPriceCents(pricingService.calculateFinalPrice(design));

        Design saved = repository.save(design);

        saveMedia(saved.getId(), request.getCoverAssetUuid(), AssetType.IMAGE, MediaRole.COVER, 0);

        if (request.getGalleryUuids() != null) {
            for (int i = 0; i < request.getGalleryUuids().size(); i++) {
                saveMedia(saved.getId(), request.getGalleryUuids().get(i),
                        AssetType.IMAGE, MediaRole.GALLERY, i + 1);
            }
        }

        saveMedia(saved.getId(), request.getPreviewVideoUuid(),
                AssetType.VIDEO, MediaRole.PREVIEW_VIDEO, 0);

        return mapper.toResponse(saved, mediaRepository.findByDesignId(saved.getId()));
    }

    /* ================= UPDATE ================= */

    @Transactional
    public DesignResponse updateDesign(Long id, DesignUpdateRequest request) {
        Design design = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Design", id));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            design.setTitle(request.getTitle());
            design.setSlug(generateUniqueSlug(request.getTitle()));
        }

        String newCoverUuid = request.getCoverAssetUuid();
        String oldCoverUuid = design.getAssetUuid();

        if (newCoverUuid != null && !newCoverUuid.isBlank() && !newCoverUuid.equals(oldCoverUuid)) {
            if (oldCoverUuid != null) {
                assetClientService.deleteAsset(oldCoverUuid);
            }
            design.setAssetUuid(newCoverUuid);
        }

        if (request.getDescription() != null) design.setDescription(request.getDescription());
        if (request.getCategoryId() != null) design.setCategoryId(request.getCategoryId());

        if (request.getSegment() != null) {
            design.setSegment(Segment.valueOf(request.getSegment().toUpperCase()));
        }

        if (request.getBasePriceCents() != null) design.setBasePriceCents(request.getBasePriceCents());
        if (request.getDiscountPercent() != null) design.setDiscountPercent(request.getDiscountPercent());

        design.setFinalPriceCents(pricingService.calculateFinalPrice(design));

        if (request.getActive() != null) design.setActive(request.getActive());
        if (request.getDraft() != null) design.setDraft(request.getDraft());
        if (request.getTrending() != null) design.setTrending(request.getTrending());
        if (request.getEditorsPick() != null) design.setEditorsPick(request.getEditorsPick());
        if (request.getNewArrival() != null) design.setNewArrival(request.getNewArrival());
        if (request.getPremium() != null) design.setPremium(request.getPremium());
        if (request.getSpecialOffer() != null) design.setSpecialOffer(request.getSpecialOffer());

        updateDesignMedia(design.getId(), request);

        return mapper.toResponse(repository.save(design), mediaRepository.findByDesignId(id));
    }

    @Transactional
    public void markDesignAsSold(Long designId) {
        Design design = repository.findById(designId)
                .orElseThrow(() -> new ResourceNotFoundException("Design", designId));
        design.setActive(false);
        design.setDraft(true);
        repository.save(design);
        log.info("🔒 Design {} marked as SOLD (Inactive + Draft)", designId);
    }

    @Transactional
    public void purgeDesignAndRecord(Long designId, Long orderId) {
        Design design = repository.findById(designId)
                .orElseThrow(() -> new ResourceNotFoundException("Design", designId));

        // 1. Capture SKU/Identifier before the design is removed from DB
        String sku = design.getDesignIdentifier();

        List<DesignMedia> mediaList = mediaRepository.findByDesignId(designId);
        List<String> assetUuids = mediaList.stream()
                .map(DesignMedia::getAssetUuid)
                .toList();

        // 2. Physical Deletion
        for (String uuid : assetUuids) {
            try {
                assetClientService.deleteAsset(uuid);
            } catch (Exception ex) {
                log.warn("⚠️ Failed to delete asset {} for design {}", uuid, designId);
            }
        }

        // 3. Clear Database
        mediaRepository.deleteAll(mediaList);
        repository.delete(design);

        // 4. Create Audit Record with the captured SKU
        DesignDeletionRecord record = DesignDeletionRecord.builder()
                .designId(designId)
                .designIdentifier(sku) // ✅ Persisting the SKU here
                .orderId(orderId)
                .deletedBy("order-service")
                .deletedAssetsCsv(String.join(",", assetUuids))
                .deletedAt(Instant.now())
                .build();

        deletionRecordRepository.save(record);
        log.info("🔥 Design {} (SKU: {}) fully purged and audit record saved for Order {}", designId, sku, orderId);
    }

    /* ================= DELETE ================= */

    @Transactional
    public void deleteDesign(Long designId) {
        Design design = repository.findById(designId)
                .orElseThrow(() -> new ResourceNotFoundException("Design not found with ID: " + designId));

        log.info("🗑️ Deleting Design ID {} and all associated assets", designId);

        List<DesignMedia> mediaList = mediaRepository.findByDesignId(designId);
        for (DesignMedia media : mediaList) {
            assetClientService.deleteAsset(media.getAssetUuid());
        }

        mediaRepository.deleteAll(mediaList);
        repository.delete(design);

        log.info("✅ Design and associated assets deleted permanently");
    }

    /* ================= MEDIA ================= */

    private void updateDesignMedia(Long designId, DesignUpdateRequest request) {
        if (request.getCoverAssetUuid() != null)
            replaceMediaRole(designId, MediaRole.COVER, List.of(request.getCoverAssetUuid()), AssetType.IMAGE);

        if (request.getGalleryUuids() != null)
            replaceMediaRole(designId, MediaRole.GALLERY, request.getGalleryUuids(), AssetType.IMAGE);

        if (request.getPreviewVideoUuid() != null)
            replaceMediaRole(designId, MediaRole.PREVIEW_VIDEO, List.of(request.getPreviewVideoUuid()), AssetType.VIDEO);
    }

    private void replaceMediaRole(Long designId, MediaRole role, List<String> uuids, AssetType type) {
        List<DesignMedia> existing = mediaRepository.findByDesignId(designId)
                .stream()
                .filter(m -> m.getMediaRole() == role)
                .collect(Collectors.toList());

        for (DesignMedia media : existing) {
            assetClientService.deleteAsset(media.getAssetUuid());
        }

        mediaRepository.deleteAll(existing);
        mediaRepository.flush();

        for (int i = 0; i < uuids.size(); i++) {
            saveMedia(designId, uuids.get(i), type, role, i);
        }
    }

    private void saveMedia(Long designId, String uuid, AssetType type, MediaRole role, int order) {
        if (uuid == null || uuid.isBlank()) return;
        assetClientService.validateAsset(uuid);
        mediaRepository.save(DesignMedia.builder()
                .designId(designId)
                .assetUuid(uuid)
                .assetType(type)
                .mediaRole(role)
                .sortOrder(order)
                .build());
    }

    private String generateUniqueSlug(String title) {
        String base = SlugGenerator.generateSlug(title);
        String slug = base;
        int i = 1;
        while (repository.existsBySlug(slug)) {
            slug = base + "-" + i++;
        }
        return slug;
    }
}