package com.rdc.admin.controller;

import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.entity.Design;
import com.rdc.admin.entity.Segment;
import com.rdc.admin.repository.DesignRepository;
import com.rdc.admin.repository.DesignMediaRepository;
import com.rdc.admin.service.DesignService;
import com.rdc.admin.util.DesignMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/designs")
@RequiredArgsConstructor
public class PublicDesignController {

    private final DesignService designService;
    private final DesignRepository designRepository; // Directly needed for search query
    private final DesignMediaRepository mediaRepository;
    private final DesignMapper mapper;

    /**
     * ✅ ENHANCED FEED: Handles Search, Segments, and Limits
     * This is the endpoint the SearchOverlay.tsx calls.
     */
    @GetMapping("/feed")
    public ResponseEntity<List<DesignResponse>> getPublicFeed(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String segment,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<Design> designs;

        // 1. Filter by Search (Title or Tags)
        if (search != null && !search.isBlank()) {
            designs = designRepository.searchByTitleOrTags(search.trim());
        } else {
            designs = designRepository.findByDraftFalseAndActiveTrue();
        }

        // 2. Filter by Segment (if provided)
        if (segment != null && !segment.isBlank()) {
            try {
                Segment seg = Segment.valueOf(segment.toUpperCase());
                designs = designs.stream()
                        .filter(d -> d.getSegment() == seg)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Ignore invalid segments or return empty
            }
        }

        // 3. Map to Response DTO with Media and apply size limit
        List<DesignResponse> response = designs.stream()
                .limit(size)
                .map(d -> mapper.toResponse(d, mediaRepository.findByDesignId(d.getId())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<DesignResponse> getPublicDesignBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(designService.getDesignBySlug(slug));
    }

    @GetMapping("/segment/{segment}")
    public ResponseEntity<List<DesignResponse>> getBySegment(@PathVariable String segment) {
        return ResponseEntity.ok(designService.getBySegment(segment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getDesignById(@PathVariable Long id) {
        DesignResponse response = designService.getDesignById(id);

        if (response == null || Boolean.TRUE.equals(response.getDraft())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}