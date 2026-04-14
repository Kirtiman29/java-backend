package com.rdc.admin.controller;

import com.rdc.admin.dto.FabricResponse;
import com.rdc.admin.service.FabricService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/fabrics")
public class InternalFabricController {

    private final FabricService fabricService;

    @Value("${internal.service.key}")
    private String internalKey;

    @GetMapping("/{id}")
    public FabricResponse getFabric(
            @PathVariable Long id,
            @RequestHeader("X-INTERNAL-KEY") String key
    ) {
        validate(key);
        return fabricService.getById(id);
    }

    @PostMapping("/{id}/reduce-stock")
    public void reduceStock(
            @PathVariable Long id,
            @RequestParam Double meters,
            @RequestHeader("X-INTERNAL-KEY") String key
    ) {
        validate(key);
        fabricService.updateStock(id, meters);
    }

    private void validate(String key) {
        if (!internalKey.equals(key)) {
            throw new RuntimeException("Invalid internal key");
        }
    }
}