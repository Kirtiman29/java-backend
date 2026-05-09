package com.rdc.admin.controller;

import com.rdc.admin.service.BlogService;
import com.rdc.admin.service.CategoryService;
import com.rdc.admin.service.DesignService;
import com.rdc.admin.service.FabricService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/slugs")
public class SlugBackfillController {

    private final DesignService designService;
    private final FabricService fabricService;
    private final CategoryService categoryService;
    private final BlogService blogService;

    @PostMapping("/backfill")
    public ResponseEntity<Map<String, Object>> backfill() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("designs", designService.backfillMissingSlugs());
        response.put("fabrics", fabricService.backfillMissingSlugs());
        response.put("categories", categoryService.backfillMissingSlugs());
        response.put("blogs", blogService.backfillMissingSlugs());
        return ResponseEntity.ok(response);
    }
}
