package com.rdc.admin.service;

import com.rdc.admin.dto.BundleCreateRequest; // <-- NEW IMPORT
import com.rdc.admin.dto.BundleDto;         // <-- NEW IMPORT
import com.rdc.admin.entity.Bundle;
import com.rdc.admin.repository.BundleRepository;
import com.rdc.admin.repository.DesignRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BundleService {

    private final BundleRepository bundleRepository;
    private final DesignRepository designRepository;

    public BundleService(BundleRepository bundleRepository, DesignRepository designRepository) {
        this.bundleRepository = bundleRepository;
        this.designRepository = designRepository;
    }

    // --- Mapper uses the real BundleDto ---
    private BundleDto mapToDto(Bundle entity) {
        // Ensure designs are loaded before trying to map IDs
        List<Long> designIds = entity.getDesigns().stream()
                .map(d -> d.getId())
                .collect(Collectors.toList());

        return BundleDto.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .priceCents(entity.getPriceCents())
                .designIds(designIds)
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .build();
    }
    // --- End Mapper ---

    public BundleDto createBundle(BundleCreateRequest request) {
        Bundle bundle = new Bundle();
        bundle.setTitle(request.getTitle());
        bundle.setDescription(request.getDescription());
        bundle.setPriceCents(request.getPriceCents());
        bundle.setCreatedAt(LocalDateTime.now());
        bundle.setActive(request.getActive());

        // Load Design entities from IDs
        if (request.getDesignIds() != null && !request.getDesignIds().isEmpty()) {
            List<Long> requestDesignIds = request.getDesignIds();

            // Check if all requested design IDs exist
            List<Long> existingDesignIds = designRepository.findAllById(requestDesignIds).stream()
                    .map(d -> d.getId())
                    .collect(Collectors.toList());

            if (existingDesignIds.size() != requestDesignIds.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more designs in the bundle were not found.");
            }

            bundle.setDesigns(designRepository.findAllById(requestDesignIds));
        }

        return mapToDto(bundleRepository.save(bundle));
    }

    // TODO: Add updateBundle method here

    public List<BundleDto> findAll() {
        return bundleRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public void deleteBundle(Long id) {
        if (!bundleRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bundle not found.");
        }
        bundleRepository.deleteById(id);
    }
}