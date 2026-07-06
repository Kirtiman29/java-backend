package com.rdc.subscription.controller;

import com.rdc.subscription.dto.bitmap.BitmapAnalyzeResponse;
import com.rdc.subscription.dto.bitmap.BitmapUploadResponse;
import com.rdc.subscription.dto.bitmap.GeminiImageToImageResponse;
import com.rdc.subscription.service.BitmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bitmap")
public class BitmapController {

    private final BitmapService bitmapService;

    @PostMapping("/upload")
    public ResponseEntity<BitmapUploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.uploadImage(file, userId));
    }

    @PostMapping("/analyze")
    public ResponseEntity<BitmapAnalyzeResponse> analyzeImage(
            @RequestParam("filename") String filename,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.analyzeImage(filename, userId));
    }

    @PostMapping("/preview/halftone")
    public ResponseEntity<byte[]> previewHalftone(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bitmapService.forwardBinaryRequest("/halftone/monochrome", params, userId);
    }

    @PostMapping("/preview/dither")
    public ResponseEntity<byte[]> previewDither(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bitmapService.forwardBinaryRequest("/dither/", params, userId);
    }

    @PostMapping("/preview/separation-proof")
    public ResponseEntity<byte[]> previewSeparationProof(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bitmapService.forwardBinaryRequest("/halftone/separation/proof", params, userId);
    }

    @PostMapping("/export/separation-zip")
    public ResponseEntity<byte[]> exportSeparationZip(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bitmapService.forwardBinaryRequest("/halftone/separation", params, userId);
    }

    @PostMapping("/export/psd")
    public ResponseEntity<byte[]> exportPsd(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bitmapService.forwardBinaryRequest("/halftone/separation/psd", params, userId);
    }

    @PostMapping("/export/cmyk")
    public ResponseEntity<byte[]> exportCmyk(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bitmapService.forwardBinaryRequest("/halftone/cmyk/", params, userId);
    }

    @PostMapping(value = "/gemini-image/image-to-image", consumes = "multipart/form-data")
    public ResponseEntity<GeminiImageToImageResponse> geminiImageToImage(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "mask_file", required = false) MultipartFile maskFile,
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.geminiImageToImage(file, maskFile, params, userId));
    }
}
