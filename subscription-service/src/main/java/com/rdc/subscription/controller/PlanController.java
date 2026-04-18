package com.rdc.subscription.controller;

import com.rdc.subscription.dto.PlanResponse;
import com.rdc.subscription.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/subscriptions")
public class PlanController {

    private final PlanService planService;

    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> getPlans() {
        return ResponseEntity.ok(planService.getActivePlans());
    }
}