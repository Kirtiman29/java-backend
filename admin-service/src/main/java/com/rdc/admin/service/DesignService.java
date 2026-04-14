package com.rdc.admin.service;

import com.rdc.admin.dto.*;
import com.rdc.admin.entity.*;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.CategoryRepository;
import com.rdc.admin.repository.DesignDeletionRecordRepository;
import com.rdc.admin.repository.DesignMediaRepository;
import com.rdc.admin.repository.DesignRepository;
import com.rdc.admin.util.DesignMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.rdc.admin.util.CsvParserUtil;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DesignService {

    private final DesignRepository repository;
    private final DesignMediaRepository mediaRepository;
    private final DesignDeletionRecordRepository deletionRecordRepository;
    private final CategoryRepository categoryRepository;
    private final DesignMapper mapper;
    private final DesignPricingService pricingService;
    private final AssetClientService assetClientService;

    /* ================= READ ================= */

    @Transactional(readOnly = true)
    public List<DesignResponse> getAllDesigns() {
        return repository.findByDraftFalseAndActiveTrue()
                .stream()
                .map(d -> (DesignResponse) mapper.toResponse(d, mediaRepository.findByDesignId(d.getId())))
                .collect(Collectors.toList());
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
            String seg = Segment.valueOf(segment.toUpperCase()).name();

            return repository.findByDraftFalseAndActiveTrue()
                    .stream()
                    .filter(d -> d.getSegment() != null)
                    .filter(d -> java.util.Arrays.stream(d.getSegment().split(","))
                            .map(String::trim)
                            .anyMatch(saved -> saved.equalsIgnoreCase(seg)))
                    .filter(d -> !Boolean.TRUE.equals(d.getDraft()))
                    .map(d -> (DesignResponse) mapper.toResponse(d, mediaRepository.findByDesignId(d.getId())))
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid segment");
        }
    }

    /* ================= BULK UPLOAD ================= */

    @Transactional
    public BulkUploadResponse processBulk(InputStream csvStream, MultipartFile[] assets) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try {
            // 1. Parse CSV
            List<DesignCreateRequest> designRequests = CsvParserUtil.parse(csvStream);

            // DEBUG LOG: Check if any rows were actually parsed
            log.info("Parsed {} rows from CSV bulk upload", designRequests.size());

            if (designRequests.isEmpty()) {
                errors.add("The CSV file appears to be empty or has incorrect headers.");
                return BulkUploadResponse.builder().successCount(0).failureCount(0).errors(errors).build();
            }

            // 2. Map files by identifier (D001 -> Files)
            Map<String, List<MultipartFile>> assetMap = new HashMap<>();
            if (assets != null) {
                for (MultipartFile file : assets) {
                    String filename = file.getOriginalFilename();
                    if (filename != null && filename.contains("_")) {
                        String identifier = filename.split("_")[0].toUpperCase();
                        assetMap.computeIfAbsent(identifier, k -> new ArrayList<>()).add(file);
                    }
                }
            }

            // 3. Process each design
            for (DesignCreateRequest req : designRequests) {
                try {
                    String idKey = req.getDesignIdentifier().toUpperCase();
                    processSingleBulkDesign(req, assetMap.get(idKey));
                    successCount++;
                } catch (Exception e) {
                    errors.add("Error processing " + req.getDesignIdentifier() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            errors.add("Fatal CSV Error: " + e.getMessage());
        }

        return BulkUploadResponse.builder()
                .successCount(successCount)
                .failureCount(errors.size())
                .errors(errors)
                .build();
    }

    public BulkUploadResponse bulkUploadFromGoogleSheet(String sheetLink, MultipartFile[] assets) {
        try {
            // 1. Clean the link and ensure it points to the CSV export
            String csvUrl = sheetLink.split("/edit")[0] + "/export?format=csv";
            
            // 2. If you need a specific Tab (GID), handle the character joining
            if (sheetLink.contains("gid=")) {
                String gid = sheetLink.substring(sheetLink.indexOf("gid="));
                csvUrl += "&" + gid; // Use '&' because 'format=csv' already used '?'
            }

            log.info("🔗 Fetching CSV from: {}", csvUrl);
            InputStream stream = java.net.URI.create(csvUrl).toURL().openStream();
            return processBulk(stream, assets); 
        } catch (Exception e) {
            return BulkUploadResponse.builder()
                    .failureCount(1)
                    .errors(List.of("Sheet Fetch Error: " + e.getMessage()))
                    .build();
        }
    }

    private void processSingleBulkDesign(DesignCreateRequest req, List<MultipartFile> files) {
        List<String> galleryUuids = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null) {
                    continue;
                }

                String name = originalFilename.toLowerCase();
                AssetResponse uploaded = assetClientService.upload(file,
                        name.contains("video") ? AssetType.VIDEO : AssetType.IMAGE);

                if (name.contains("cover")) {
                    req.setCoverAssetUuid(uploaded.getUuid());
                } else if (name.contains("gallery")) {
                    galleryUuids.add(uploaded.getUuid());
                } else if (name.contains("video")) {
                    req.setPreviewVideoUuid(uploaded.getUuid());
                }
            }
        }

        // Check UPSERT
        Optional<Design> existing = repository.findByDesignIdentifier(req.getDesignIdentifier());
        if (existing.isPresent()) {
            if (!galleryUuids.isEmpty()) {
                req.setGalleryUuids(galleryUuids);
            }
            updateDesign(existing.get().getId(), mapToUpdate(req));
        } else {
            if (req.getCoverAssetUuid() == null || req.getCoverAssetUuid().isBlank()) {
                throw new RuntimeException("Missing mandatory cover image for new design");
            }
            req.setGalleryUuids(galleryUuids);
            createDesign(req);
        }
    }

    private DesignUpdateRequest mapToUpdate(DesignCreateRequest req) {
        DesignUpdateRequest update = new DesignUpdateRequest();
        update.setDesignIdentifier(req.getDesignIdentifier());
        update.setTitle(req.getTitle());
        update.setDescription(req.getDescription());
        update.setBasePriceCents(req.getBasePriceCents());
        update.setCategoryIds(req.getCategoryIds());
        update.setRepeatSize(req.getRepeatSize());
        update.setDesignType(req.getDesignType());
        update.setImageType(req.getImageType());
        update.setImageFormat(req.getImageFormat());
        update.setColorCount(req.getColorCount());
        update.setResolution(req.getResolution());
        update.setTags(req.getTags());
        update.setSegments(req.getSegments());
        update.setActive(req.getActive());
        update.setDraft(req.getDraft());
        update.setTrending(req.getTrending());
        update.setEditorsPick(req.getEditorsPick());
        update.setNewArrival(req.getNewArrival());
        update.setLuxury(req.getLuxury());
        update.setDiscountPercent(req.getDiscountPercent());
        update.setSpecialOffer(req.getSpecialOffer());
        if (req.getCoverAssetUuid() != null && !req.getCoverAssetUuid().isBlank()) {
            update.setCoverAssetUuid(req.getCoverAssetUuid());
        }
        if (req.getGalleryUuids() != null && !req.getGalleryUuids().isEmpty()) {
            update.setGalleryUuids(req.getGalleryUuids());
        }
        if (req.getPreviewVideoUuid() != null && !req.getPreviewVideoUuid().isBlank()) {
            update.setPreviewVideoUuid(req.getPreviewVideoUuid());
        }
        return update;
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
        design.setDescription(request.getDescription());
        design.setDesignIdentifier(request.getDesignIdentifier());
        design.setBasePriceCents(request.getBasePriceCents());
        design.setAssetUuid(request.getCoverAssetUuid());

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            design.getCategories().clear();
            design.getCategories().addAll(categories);
        }

        // Industrial Specifications
        design.setRepeatSize(request.getRepeatSize());
        design.setDesignType(request.getDesignType());
        design.setImageType(request.getImageType());
        design.setImageFormat(request.getImageFormat());
        design.setColorCount(request.getColorCount());
        design.setResolution(request.getResolution());

        if (request.getTags() != null) {
            design.getTags().clear();
            design.getTags().addAll(request.getTags());
        }

        design.setSegment(joinSegments(request.getSegments()));

        design.setActive(Boolean.TRUE.equals(request.getActive()));
        design.setDraft(Boolean.TRUE.equals(request.getDraft()));
        design.setTrending(Boolean.TRUE.equals(request.getTrending()));
        design.setEditorsPick(Boolean.TRUE.equals(request.getEditorsPick()));
        design.setNewArrival(Boolean.TRUE.equals(request.getNewArrival()));
        design.setLuxury(Boolean.TRUE.equals(request.getLuxury()));
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
        }

        if (request.getDesignIdentifier() != null && !request.getDesignIdentifier().isBlank()) {
            design.setDesignIdentifier(request.getDesignIdentifier());
        }

        String newCoverUuid = request.getCoverAssetUuid();
        String oldCoverUuid = design.getAssetUuid();

        if (newCoverUuid != null && !newCoverUuid.isBlank() && !newCoverUuid.equals(oldCoverUuid)) {
            if (oldCoverUuid != null && !oldCoverUuid.equals(newCoverUuid)) {
                assetClientService.deleteAsset(oldCoverUuid);
            }
            design.setAssetUuid(newCoverUuid);
        }

        if (request.getDescription() != null) design.setDescription(request.getDescription());

        if (request.getCategoryIds() != null) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            design.getCategories().clear();
            design.getCategories().addAll(categories);
        }

        if (request.getRepeatSize() != null) design.setRepeatSize(request.getRepeatSize());
        if (request.getDesignType() != null) design.setDesignType(request.getDesignType());
        if (request.getImageFormat() != null) design.setImageFormat(request.getImageFormat());
        if (request.getColorCount() != null) design.setColorCount(request.getColorCount());
        if (request.getResolution() != null) design.setResolution(request.getResolution());
        if (request.getImageType() != null) design.setImageType(request.getImageType());

        if (request.getTags() != null) {
            design.getTags().clear();
            design.getTags().addAll(request.getTags());
        }

        if (request.getSegments() != null) {
            design.setSegment(joinSegments(request.getSegments()));
        }

        if (request.getBasePriceCents() != null) design.setBasePriceCents(request.getBasePriceCents());
        if (request.getDiscountPercent() != null) design.setDiscountPercent(request.getDiscountPercent());

        design.setFinalPriceCents(pricingService.calculateFinalPrice(design));

        if (request.getActive() != null) design.setActive(request.getActive());
        if (request.getDraft() != null) design.setDraft(request.getDraft());
        if (request.getTrending() != null) design.setTrending(request.getTrending());
        if (request.getEditorsPick() != null) design.setEditorsPick(request.getEditorsPick());
        if (request.getNewArrival() != null) design.setNewArrival(request.getNewArrival());
        if (request.getLuxury() != null) design.setLuxury(request.getLuxury());
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

        String sku = design.getDesignIdentifier();

        List<DesignMedia> mediaList = mediaRepository.findByDesignId(designId);
        List<String> assetUuids = mediaList.stream()
                .map(DesignMedia::getAssetUuid)
                .toList();

        for (String uuid : assetUuids) {
            try {
                assetClientService.deleteAsset(uuid);
            } catch (Exception ex) {
                log.warn("⚠️ Failed to delete asset {} for design {}", uuid, designId);
            }
        }

        mediaRepository.deleteAll(mediaList);
        repository.delete(design);

        DesignDeletionRecord record = DesignDeletionRecord.builder()
                .designId(designId)
                .designIdentifier(sku)
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

    private String joinSegments(List<String> segments) {
        if (segments == null || segments.isEmpty()) {
            return null;
        }

        return segments.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private void replaceMediaRole(Long designId, MediaRole role, List<String> uuids, AssetType type) {
        List<DesignMedia> existing = mediaRepository.findByDesignId(designId)
                .stream()
                .filter(m -> m.getMediaRole() == role)
                .collect(Collectors.toList());

        for (DesignMedia media : existing) {
            if (!uuids.contains(media.getAssetUuid())) {
                assetClientService.deleteAsset(media.getAssetUuid());
            }
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
}
