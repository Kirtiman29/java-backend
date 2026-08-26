package com.rdc.subscription.controller;

import com.rdc.subscription.dto.bitmap.GeminiImageToImageResponse;
import com.rdc.subscription.service.BitmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gemini-image")
public class GeminiImageProxyController {

    private final BitmapService bitmapService;

    @PostMapping(value = "/image-to-image", consumes = "multipart/form-data")
    public ResponseEntity<GeminiImageToImageResponse> imageToImage(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "image", required = false) MultipartFile imageInput,
            @RequestParam(value = "input_image", required = false) MultipartFile inputImage,
            @RequestParam(value = "mask_file", required = false) MultipartFile maskFile,
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        MultipartFile effectiveFile = file != null ? file : (imageInput != null ? imageInput : inputImage);
        if (effectiveFile == null || effectiveFile.isEmpty()) {
            throw new IllegalArgumentException("Image file is required under parameter 'file' or 'image'.");
        }
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.geminiImageToImage(effectiveFile, maskFile, params, userId));
    }
}