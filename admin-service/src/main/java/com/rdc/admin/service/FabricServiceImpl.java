package com.rdc.admin.service;

import com.rdc.admin.dto.*;
import com.rdc.admin.entity.AssetType;
import com.rdc.admin.entity.Fabric;
import com.rdc.admin.entity.FabricMedia;
import com.rdc.admin.entity.MediaRole;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.CategoryRepository;
import com.rdc.admin.repository.FabricMediaRepository;
import com.rdc.admin.repository.FabricRepository;
import com.rdc.admin.util.CsvParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FabricServiceImpl implements FabricService {

    private final FabricRepository fabricRepository;
    private final FabricMediaRepository fabricMediaRepository;
    private final CategoryRepository categoryRepository;
    private final AssetClientService assetClientService;

    @Value("${service.asset.url}")
    private String assetServiceBaseUrl;

    @Override
    @Transactional
    public FabricResponse create(FabricCreateRequest req) {
        validateUniqueIdentifier(req.getFabricIdentifier(), null);
        validateCategory(req.getCategoryId());

        Fabric fabric = new Fabric();
        applyCreateFields(fabric, req);
        Fabric saved = fabricRepository.save(fabric);

        saveMedia(saved.getId(), req.getCoverAssetUuid(), MediaRole.COVER, 0);
        if (req.getGalleryUuids() != null) {
            for (int i = 0; i < req.getGalleryUuids().size(); i++) {
                saveMedia(saved.getId(), req.getGalleryUuids().get(i), MediaRole.GALLERY, i + 1);
            }
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public FabricResponse update(Long id, FabricUpdateRequest req) {
        Fabric fabric = fabricRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fabric", id));

        if (req.getFabricIdentifier() != null && !req.getFabricIdentifier().isBlank()) {
            validateUniqueIdentifier(req.getFabricIdentifier(), id);
            fabric.setFabricIdentifier(req.getFabricIdentifier().trim());
        }

        if (req.getTitle() != null) fabric.setTitle(req.getTitle());
        if (req.getDescription() != null) fabric.setDescription(req.getDescription());
        if (req.getPricePerMeter() != null) fabric.setPricePerMeter(req.getPricePerMeter());
        if (req.getStockMeters() != null) fabric.setStockMeters(req.getStockMeters());
        if (req.getMaterial() != null) fabric.setMaterial(req.getMaterial());
        if (req.getWidth() != null) fabric.setWidth(req.getWidth());
        if (req.getGsm() != null) fabric.setGsm(req.getGsm());
        if (req.getLength() != null) fabric.setLength(req.getLength());
        if (req.getCategoryId() != null) {
            validateCategory(req.getCategoryId());
            fabric.setCategoryId(req.getCategoryId());
        }
        if (req.getActive() != null) fabric.setActive(req.getActive());

        if (req.getCoverAssetUuid() != null) {
            replaceCover(fabric, req.getCoverAssetUuid());
        }
        if (req.getGalleryUuids() != null) {
            replaceMediaRole(fabric.getId(), MediaRole.GALLERY, req.getGalleryUuids());
        }

        return mapToResponse(fabricRepository.save(fabric));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FabricResponse> getAll() {
        return fabricRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FabricResponse> getAllAdmin() {
        return fabricRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FabricResponse getById(Long id) {
        Fabric fabric = fabricRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fabric not found: " + id));

        return mapToResponse(fabric);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Fabric fabric = fabricRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fabric", id));

        List<FabricMedia> media = fabricMediaRepository.findByFabricId(id);
        Set<String> deletedAssets = new HashSet<>();
        for (FabricMedia item : media) {
            assetClientService.deleteAsset(item.getAssetUuid());
            deletedAssets.add(item.getAssetUuid());
        }

        if (fabric.getAssetUuid() != null && !deletedAssets.contains(fabric.getAssetUuid())) {
            assetClientService.deleteAsset(fabric.getAssetUuid());
        }

        fabricMediaRepository.deleteAll(media);
        fabricRepository.delete(fabric);
    }

    @Override
    @Transactional
    public void updateStock(Long id, Double meters) {

        Fabric fabric = fabricRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fabric not found: " + id));

        if (fabric.getStockMeters() == null || fabric.getStockMeters() < meters) {
            throw new RuntimeException("Insufficient stock");
        }

        fabric.setStockMeters(fabric.getStockMeters() - meters);
        fabricRepository.save(fabric);
    }

    @Override
    @Transactional
    public BulkUploadResponse processBulk(InputStream csvStream, MultipartFile[] assets) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try {
            List<FabricCreateRequest> requests = CsvParserUtil.parseFabrics(csvStream);
            if (requests.isEmpty()) {
                errors.add("The CSV file appears to be empty or has incorrect headers.");
                return BulkUploadResponse.builder().successCount(0).failureCount(0).errors(errors).build();
            }

            Map<String, List<MultipartFile>> assetMap = buildAssetMap(assets);

            for (FabricCreateRequest req : requests) {
                try {
                    String key = resolveBulkKey(req);
                    processSingleBulkFabric(req, assetMap.get(key));
                    successCount++;
                } catch (Exception e) {
                    String label = req.getFabricIdentifier() != null && !req.getFabricIdentifier().isBlank()
                            ? req.getFabricIdentifier()
                            : req.getTitle();
                    errors.add("Error processing " + label + ": " + e.getMessage());
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

    private void processSingleBulkFabric(FabricCreateRequest req, List<MultipartFile> files) {
        List<String> galleryUuids = new ArrayList<>();

        if (files != null) {
            for (MultipartFile file : files) {
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null) {
                    continue;
                }

                AssetResponse uploaded = assetClientService.upload(file, AssetType.IMAGE);
                String lowerName = originalFilename.toLowerCase();
                if (lowerName.contains("cover")) {
                    req.setCoverAssetUuid(uploaded.getUuid());
                } else {
                    galleryUuids.add(uploaded.getUuid());
                }
            }
        }

        if (!galleryUuids.isEmpty()) {
            req.setGalleryUuids(galleryUuids);
        }

        Optional<Fabric> existing = findExisting(req);
        if (existing.isPresent()) {
            update(existing.get().getId(), mapToUpdate(req));
            return;
        }

        create(req);
    }

    private FabricUpdateRequest mapToUpdate(FabricCreateRequest req) {
        FabricUpdateRequest update = new FabricUpdateRequest();
        update.setFabricIdentifier(req.getFabricIdentifier());
        update.setTitle(req.getTitle());
        update.setDescription(req.getDescription());
        update.setPricePerMeter(req.getPricePerMeter());
        update.setStockMeters(req.getStockMeters());
        update.setMaterial(req.getMaterial());
        update.setWidth(req.getWidth());
        update.setGsm(req.getGsm());
        update.setLength(req.getLength());
        update.setCategoryId(req.getCategoryId());
        update.setActive(req.getActive());
        if (req.getCoverAssetUuid() != null && !req.getCoverAssetUuid().isBlank()) {
            update.setCoverAssetUuid(req.getCoverAssetUuid());
        }
        if (req.getGalleryUuids() != null && !req.getGalleryUuids().isEmpty()) {
            update.setGalleryUuids(req.getGalleryUuids());
        }
        return update;
    }

    private Optional<Fabric> findExisting(FabricCreateRequest req) {
        if (req.getFabricIdentifier() != null && !req.getFabricIdentifier().isBlank()) {
            return fabricRepository.findByFabricIdentifier(req.getFabricIdentifier().trim());
        }
        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            return fabricRepository.findByTitleIgnoreCase(req.getTitle().trim());
        }
        return Optional.empty();
    }

    private Map<String, List<MultipartFile>> buildAssetMap(MultipartFile[] assets) {
        Map<String, List<MultipartFile>> assetMap = new HashMap<>();
        if (assets == null) {
            return assetMap;
        }

        for (MultipartFile file : assets) {
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.contains("_")) {
                continue;
            }
            String identifier = filename.split("_")[0].trim().toUpperCase();
            assetMap.computeIfAbsent(identifier, ignored -> new ArrayList<>()).add(file);
        }

        return assetMap;
    }

    private String resolveBulkKey(FabricCreateRequest req) {
        String raw = req.getFabricIdentifier();
        if (raw == null || raw.isBlank()) {
            raw = req.getTitle();
        }
        if (raw == null || raw.isBlank()) {
            throw new RuntimeException("fabricIdentifier or title is required for bulk upload");
        }
        return raw.trim().toUpperCase();
    }

    private void applyCreateFields(Fabric fabric, FabricCreateRequest req) {
        fabric.setFabricIdentifier(clean(req.getFabricIdentifier()));
        fabric.setTitle(clean(req.getTitle()));
        fabric.setDescription(clean(req.getDescription()));
        fabric.setPricePerMeter(req.getPricePerMeter());
        fabric.setStockMeters(req.getStockMeters());
        fabric.setMaterial(clean(req.getMaterial()));
        fabric.setWidth(req.getWidth());
        fabric.setGsm(req.getGsm());
        fabric.setLength(clean(req.getLength()));
        fabric.setCategoryId(req.getCategoryId());
        fabric.setAssetUuid(clean(req.getCoverAssetUuid()));
        fabric.setActive(req.getActive() != null ? req.getActive() : Boolean.TRUE);
    }

    private void replaceCover(Fabric fabric, String newCoverAssetUuid) {
        String cleaned = clean(newCoverAssetUuid);
        if (Objects.equals(cleaned, fabric.getAssetUuid())) {
            return;
        }

        if (fabric.getAssetUuid() != null) {
            assetClientService.deleteAsset(fabric.getAssetUuid());
        }

        if (cleaned == null) {
            deleteRoleMedia(fabric.getId(), MediaRole.COVER);
            fabric.setAssetUuid(null);
            return;
        }

        assetClientService.validateAsset(cleaned);
        fabric.setAssetUuid(cleaned);
        replaceMediaRole(fabric.getId(), MediaRole.COVER, List.of(cleaned));
    }

    private void replaceMediaRole(Long fabricId, MediaRole role, List<String> uuids) {
        List<String> cleanUuids = uuids == null ? List.of() : uuids.stream()
                .map(this::clean)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<FabricMedia> existing = fabricMediaRepository.findByFabricId(fabricId)
                .stream()
                .filter(media -> media.getMediaRole() == role)
                .collect(Collectors.toList());

        for (FabricMedia media : existing) {
            if (!cleanUuids.contains(media.getAssetUuid())) {
                assetClientService.deleteAsset(media.getAssetUuid());
            }
        }

        fabricMediaRepository.deleteAll(existing);

        for (int i = 0; i < cleanUuids.size(); i++) {
            saveMedia(fabricId, cleanUuids.get(i), role, role == MediaRole.COVER ? 0 : i + 1);
        }
    }

    private void deleteRoleMedia(Long fabricId, MediaRole role) {
        List<FabricMedia> existing = fabricMediaRepository.findByFabricId(fabricId)
                .stream()
                .filter(media -> media.getMediaRole() == role)
                .toList();
        fabricMediaRepository.deleteAll(existing);
    }

    private void saveMedia(Long fabricId, String uuid, MediaRole role, int order) {
        String cleaned = clean(uuid);
        if (cleaned == null) {
            return;
        }

        assetClientService.validateAsset(cleaned);
        fabricMediaRepository.save(FabricMedia.builder()
                .fabricId(fabricId)
                .assetUuid(cleaned)
                .assetType(AssetType.IMAGE)
                .mediaRole(role)
                .sortOrder(order)
                .build());
    }

    private void validateCategory(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", categoryId);
        }
    }

    private void validateUniqueIdentifier(String fabricIdentifier, Long currentId) {
        String cleaned = clean(fabricIdentifier);
        if (cleaned == null) {
            return;
        }

        fabricRepository.findByFabricIdentifier(cleaned).ifPresent(existing -> {
            if (!existing.getId().equals(currentId)) {
                throw new RuntimeException("fabricIdentifier already exists");
            }
        });
    }

    private FabricResponse mapToResponse(Fabric fabric) {
        List<FabricMediaDto> media = fabricMediaRepository.findByFabricId(fabric.getId())
                .stream()
                .sorted(Comparator.comparing(FabricMedia::getSortOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(this::mapMedia)
                .toList();

        return FabricResponse.builder()
                .id(fabric.getId())
                .fabricIdentifier(fabric.getFabricIdentifier())
                .title(fabric.getTitle())
                .description(fabric.getDescription())
                .pricePerMeter(fabric.getPricePerMeter())
                .stockMeters(fabric.getStockMeters())
                .material(fabric.getMaterial())
                .width(fabric.getWidth())
                .gsm(fabric.getGsm())
                .length(fabric.getLength())
                .categoryId(fabric.getCategoryId())
                .assetUuid(fabric.getAssetUuid())
                .media(media)
                .active(fabric.getActive())
                .build();
    }

    private FabricMediaDto mapMedia(FabricMedia media) {
        return FabricMediaDto.builder()
                .url(assetServiceBaseUrl + "/api/assets/download/" + media.getAssetUuid())
                .type(media.getAssetType() != null ? media.getAssetType().name() : AssetType.IMAGE.name())
                .role(media.getMediaRole() != null ? media.getMediaRole().name() : MediaRole.GALLERY.name())
                .build();
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
