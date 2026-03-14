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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/public/designs")
@RequiredArgsConstructor
public class PublicDesignController {

    private final DesignService designService;
    private final DesignRepository designRepository;
    private final DesignMediaRepository mediaRepository;
    private final DesignMapper mapper;

    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> getPublicFeed(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) Long categoryId,   // ✅ NEW CATEGORY FILTER
            @RequestParam(required = false) Boolean luxury,
            @RequestParam(required = false) Boolean trending,
            @RequestParam(required = false) Boolean editorsPick,
            @RequestParam(required = false) Boolean newArrival,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {

        List<Design> designs;

        // 1️⃣ Search
        if (search != null && !search.isBlank()) {
            designs = designRepository.searchByTitleOrTags(search.trim());
        } else {
            designs = designRepository.findByDraftFalseAndActiveTrue();
        }

        // 2️⃣ Segment filter
        if (segment != null && !segment.isBlank()) {
            try {
                Segment seg = Segment.valueOf(segment.toUpperCase());

                designs = designs.stream()
                        .filter(d -> d.getSegments() != null && d.getSegments().contains(seg))
                        .collect(Collectors.toList());

            } catch (IllegalArgumentException ignored) {}
        }

        // 3️⃣ Category filter (NEW)
        if (categoryId != null) {
            designs = designRepository
                    .findByDraftFalseAndActiveTrueAndCategories_Id(categoryId);
        }

        // 4️⃣ Attribute filters

        if (Boolean.TRUE.equals(luxury)) {
            designs = designs.stream()
                    .filter(Design::getLuxury)
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(trending)) {
            designs = designs.stream()
                    .filter(Design::getTrending)
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(editorsPick)) {
            designs = designs.stream()
                    .filter(Design::getEditorsPick)
                    .collect(Collectors.toList());
        }

        if (Boolean.TRUE.equals(newArrival)) {
            designs = designs.stream()
                    .filter(Design::getNewArrival)
                    .collect(Collectors.toList());
        }

        // 5️⃣ Sort newest first (using createdAt)
        designs.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        // 6️⃣ Pagination
        int start = page * size;
        int end = Math.min(start + size, designs.size());

        List<Design> paginated = new ArrayList<>();

        if (start < designs.size()) {
            paginated = designs.subList(start, end);
        }

        // 7️⃣ Map to response
        List<DesignResponse> content = paginated.stream()
                .map(d -> mapper.toResponse(
                        d,
                        mediaRepository.findByDesignId(d.getId())
                ))
                .collect(Collectors.toList());

        // 8️⃣ Final API response
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
    public ResponseEntity<List<DesignResponse>> getBySegment(@PathVariable String segment) {
        return ResponseEntity.ok(designService.getBySegment(segment));
    }

    /**
     * Get single design
     */
    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getDesignById(@PathVariable Long id) {

        DesignResponse response = designService.getDesignById(id);

        if (response == null || Boolean.TRUE.equals(response.getDraft())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }
}