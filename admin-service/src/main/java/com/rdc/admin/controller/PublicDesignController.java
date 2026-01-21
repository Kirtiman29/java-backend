package com.rdc.admin.controller;

import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/designs") // ✅ Matched to SecurityConfig .permitAll()
@RequiredArgsConstructor
public class PublicDesignController {

    private final DesignService designService;

    // Card Gallery / Storefront Feed
    @GetMapping("/feed")
    public ResponseEntity<List<DesignResponse>> getPublicFeed() {
        // Returns designs where role = COVER
        return ResponseEntity.ok(designService.getAllDesigns());
    }

    // Detail Page using SEO Slugs
    @GetMapping("/slug/{slug}")
    public ResponseEntity<DesignResponse> getPublicDesignBySlug(@PathVariable String slug) {
        // Returns full gallery where MediaRole is GALLERY or PREVIEW_VIDEO
        return ResponseEntity.ok(designService.getDesignBySlug(slug));
    }

    // Navigation Segments (Menswear, etc.)
    @GetMapping("/segment/{segment}")
    public ResponseEntity<List<DesignResponse>> getBySegment(@PathVariable String segment) {
        return ResponseEntity.ok(designService.getBySegment(segment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getDesignById(@PathVariable Long id) {
        DesignResponse response = designService.getDesignById(id);
        // Security check: Never show drafts to the public [cite: 159, 160]
        if (Boolean.TRUE.equals(response.getDraft())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}