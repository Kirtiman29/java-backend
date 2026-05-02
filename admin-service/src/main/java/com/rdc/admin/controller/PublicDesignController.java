package com.rdc.admin.controller;

import com.rdc.admin.client.SubscriptionServiceClient;
import com.rdc.admin.dto.DesignDownloadResponse;
import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.entity.Design;
import com.rdc.admin.repository.DesignRepository;
import com.rdc.admin.repository.DesignMediaRepository;
import com.rdc.admin.service.DesignDownloadService;
import com.rdc.admin.service.DesignService;
import com.rdc.admin.util.DesignMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/designs")
@RequiredArgsConstructor
public class PublicDesignController {

    private final DesignService designService;
    private final DesignDownloadService designDownloadService;
    private final SubscriptionServiceClient subscriptionServiceClient;
    private final DesignRepository designRepository;
    private final DesignMediaRepository mediaRepository;
    private final DesignMapper mapper;

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> getPublicFeed(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean luxury,
            @RequestParam(required = false) Boolean trending,
            @RequestParam(required = false) Boolean editorsPick,
            @RequestParam(required = false) Boolean newArrival,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        boolean hasSubscriptionAccess = hasSubscriptionAccess(jwt);

        List<Design> designs = new ArrayList<>();

        // 1️⃣ Search
        if (search != null && !search.isBlank()) {
            designs = Optional.ofNullable(
                    designRepository.searchByTitleOrTags(search.trim())
            ).orElse(new ArrayList<>());
        } else {
            designs = Optional.ofNullable(
                    designRepository.findByDraftFalseAndActiveTrue()
            ).orElse(new ArrayList<>());
        }

        // 2️⃣ Segment filter (✅ FIXED: Updated to use the single 'segment' string field)
        if (segment != null && !segment.isBlank()) {
            String segFilter = segment.toUpperCase();
            designs = designs.stream()
                    .filter(Objects::nonNull)
                    .filter(d -> hasSegment(d, segFilter))
                    .collect(Collectors.toList());
        }

        // 3️⃣ Category filter
        if (categoryId != null) {
            designs = Optional.ofNullable(
                    designRepository.findByDraftFalseAndActiveTrueAndCategories_Id(categoryId)
            ).orElse(new ArrayList<>());
        }

        // 4️⃣ Attribute filters
        if (Boolean.TRUE.equals(luxury)) {
            designs = designs.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getLuxury()))
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(trending)) {
            designs = designs.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getTrending()))
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(editorsPick)) {
            designs = designs.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getEditorsPick()))
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(newArrival)) {
            designs = designs.stream()
                    .filter(d -> Boolean.TRUE.equals(d.getNewArrival()))
                    .collect(Collectors.toList());
        }

        designs = filterVisibleDesigns(designs, hasSubscriptionAccess);

        // 5️⃣ Sort newest first
        designs.sort(
                Comparator.comparing(
                        Design::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        // 6️⃣ Pagination
        int start = page * size;
        int end = Math.min(start + size, designs.size());

        List<Design> paginated = new ArrayList<>();

        if (start < designs.size()) {
            paginated = designs.subList(start, end);
        }

        // 7️⃣ Map to response
        List<DesignResponse> content = paginated.stream()
                .filter(Objects::nonNull)
                .map(d -> mapper.toResponse(
                        d,
                        mediaRepository.findByDesignId(d.getId())
                ))
                .collect(Collectors.toList());

        // 8️⃣ Response
        Map<String, Object> result = new HashMap<>();

        result.put("content", content);
        result.put("totalElements", designs.size());
        result.put("page", page);
        result.put("size", size);

        return ResponseEntity.ok(result);
    }

    /**
     * Get designs by segment
     */
    @GetMapping("/segment/{segment}")
    public ResponseEntity<List<DesignResponse>> getBySegment(
            @PathVariable String segment,
            @AuthenticationPrincipal Jwt jwt
    ) {
        boolean hasSubscriptionAccess = hasSubscriptionAccess(jwt);
        List<DesignResponse> visibleDesigns = designService.getBySegment(segment).stream()
                .filter(design -> isVisibleToUser(design.getSubscriptionOnly(), hasSubscriptionAccess))
                .toList();
        return ResponseEntity.ok(visibleDesigns);
    }

    /**
     * Get single design
     */
    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getDesignById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt
    ) {

        DesignResponse response = designService.getDesignById(id);

        if (response == null || Boolean.TRUE.equals(response.getDraft())) {
            return ResponseEntity.notFound().build();
        }

        if (!isVisibleToUser(response.getSubscriptionOnly(), hasSubscriptionAccess(jwt))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active design subscription required");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/download/{designId}")
    public ResponseEntity<DesignDownloadResponse> downloadDesign(
            @PathVariable Long designId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(designDownloadService.requestDownload(userId, designId));
    }

    private boolean hasSegment(Design design, String segment) {
        if (design.getSegment() == null || design.getSegment().isBlank()) {
            return false;
        }

        return Arrays.stream(design.getSegment().split(","))
                .map(String::trim)
                .anyMatch(saved -> saved.equalsIgnoreCase(segment));
    }

    private List<Design> filterVisibleDesigns(List<Design> designs, boolean hasSubscriptionAccess) {
        return designs.stream()
                .filter(d -> isVisibleToUser(d.getSubscriptionOnly(), hasSubscriptionAccess))
                .collect(Collectors.toList());
    }

    private boolean isVisibleToUser(Boolean subscriptionOnly, boolean hasSubscriptionAccess) {
        return !Boolean.TRUE.equals(subscriptionOnly) || hasSubscriptionAccess;
    }

    private boolean hasSubscriptionAccess(Jwt jwt) {
        if (jwt == null) {
            return false;
        }

        Long userId = Long.parseLong(jwt.getSubject());
        return subscriptionServiceClient.hasActiveDesignAccess(userId);
    }
}
