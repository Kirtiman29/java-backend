package com.rdc.admin.controller;

import com.rdc.admin.dto.AssetResponse;
import com.rdc.admin.entity.AssetType;
import com.rdc.admin.service.AssetClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/assets")
public class AdminAssetController {

    private final AssetClientService assetClientService;

    @PostMapping("/upload")
    public ResponseEntity<AssetResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") AssetType type) {

        return new ResponseEntity<>(assetClientService.upload(file, type), HttpStatus.CREATED);
    }
}
