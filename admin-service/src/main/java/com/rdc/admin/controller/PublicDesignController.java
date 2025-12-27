package com.rdc.admin.controller;

import com.rdc.admin.dto.DesignResponse;
import com.rdc.admin.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public Design Controller
 *
 * This controller exposes design endpoints WITHOUT authentication.
 * Used by Cart Service to fetch design info and prices.
 */
@RestController
@RequestMapping("/api/public/designs")
@RequiredArgsConstructor
public class PublicDesignController {

    private final DesignService designService;

    /**
     * Get design by ID - PUBLIC (no auth required)
     * Used by Cart Service to fetch price and validate design
     */
    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getDesignById(@PathVariable Long id) {
        return ResponseEntity.ok(designService.getDesignById(id));
    }
}