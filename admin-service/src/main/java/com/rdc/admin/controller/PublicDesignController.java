package com.rdc.admin.controller;

import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/public/designs")
@RequiredArgsConstructor
public class PublicDesignController {

    private final DesignService designService;

    @GetMapping("/segment/{segment}")
    public ResponseEntity<List<DesignResponse>> getBySegment(@PathVariable String segment) {
        return ResponseEntity.ok(designService.getBySegment(segment));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getDesignById(@PathVariable Long id) {
        DesignResponse response = designService.getDesignById(id);
        // Security check: Don't show drafts publicly
        if (Boolean.TRUE.equals(response.getDraft())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}