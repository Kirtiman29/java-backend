package com.rdc.admin.service;

import com.rdc.admin.dto.BannerRequest;
import com.rdc.admin.dto.BannerResponse;
import com.rdc.admin.entity.BannerTheme;
import com.rdc.admin.entity.HomepageBanner;
import com.rdc.admin.exception.ResourceNotFoundException;
import com.rdc.admin.repository.HomepageBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final HomepageBannerRepository repository;
    private final AssetClientService assetClientService;

    @Transactional(readOnly = true)
    public BannerResponse getActiveBanner() {
        LocalDate today = LocalDate.now();

        return repository.findActiveBanners(today)
                .stream()
                .findFirst()
                .map(this::mapToResponse)
                .orElseGet(() -> repository.findByTheme(BannerTheme.DEFAULT)
                        .map(this::mapToResponse)
                        .orElse(null));
    }

    @Transactional
    public BannerResponse createBanner(BannerRequest request) {
        if (request.getBackgroundImageUuid() != null) {
            assetClientService.validateAsset(request.getBackgroundImageUuid());
        }

        HomepageBanner banner = new HomepageBanner();
        updateBannerFields(banner, request);

        String imageUrl = "http://localhost:8090/api/assets/" + request.getBackgroundImageUuid() + "/download";
        banner.setBackgroundImageUrl(imageUrl);

        return mapToResponse(repository.save(banner));
    }

    @Transactional
    public BannerResponse updateBanner(Long id, BannerRequest request) {
        HomepageBanner banner = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found"));

        if (request.getBackgroundImageUuid() != null) {
            assetClientService.validateAsset(request.getBackgroundImageUuid());
            banner.setBackgroundImageUrl("http://localhost:8090/api/assets/" + request.getBackgroundImageUuid() + "/download");
        }

        updateBannerFields(banner, request);
        return mapToResponse(repository.save(banner));
    }

    @Transactional // ✅ FIXED: Added Delete Logic
    public void deleteBanner(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Banner not found with id: " + id);
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void updateBannerFields(HomepageBanner banner, BannerRequest request) {
        banner.setSubtitle(request.getSubtitle());
        banner.setTitle(request.getTitle());
        banner.setDescription(request.getDescription());
        banner.setCtaText(request.getCtaText());
        banner.setCtaUrl(request.getCtaUrl());
        banner.setTheme(BannerTheme.valueOf(request.getTheme().toUpperCase()));
        banner.setStartDate(request.getStartDate());
        banner.setEndDate(request.getEndDate());
        banner.setActive(request.getActive());
        banner.setPriority(request.getPriority());
    }

    private BannerResponse mapToResponse(HomepageBanner entity) {
        return BannerResponse.builder()
                .id(entity.getId())
                .subtitle(entity.getSubtitle())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .ctaText(entity.getCtaText())
                .ctaUrl(entity.getCtaUrl())
                .backgroundImageUrl(entity.getBackgroundImageUrl())
                .theme(entity.getTheme().name())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .active(entity.getActive())
                .priority(entity.getPriority())
                .build();
    }
}