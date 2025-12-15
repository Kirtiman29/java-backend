package com.rdc.asset.controller;

import com.rdc.asset.dto.AssetDto;
import com.rdc.asset.dto.AssetRequest;
import com.rdc.asset.service.AssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @PostMapping
    public ResponseEntity<AssetDto> createAsset(@Valid @RequestBody AssetRequest request) {
        return ResponseEntity.ok(assetService.createAsset(request));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<AssetDto> getAsset(@PathVariable String uuid) {
        return ResponseEntity.ok(assetService.getAssetByUuid(uuid));
    }

    @GetMapping
    public ResponseEntity<List<AssetDto>> getAll() {
        return ResponseEntity.ok(assetService.getAllAssets());
    }
}
