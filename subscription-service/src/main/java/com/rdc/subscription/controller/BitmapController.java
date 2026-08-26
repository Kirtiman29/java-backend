package com.rdc.subscription.controller;

import com.rdc.subscription.dto.bitmap.BitmapAnalyzeResponse;
import com.rdc.subscription.dto.bitmap.BitmapJobResponse;
import com.rdc.subscription.dto.bitmap.BitmapUploadResponse;
import com.rdc.subscription.dto.bitmap.GeminiImageToImageResponse;
import com.rdc.subscription.service.BitmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
    public ResponseEntity<BitmapJobResponse> previewHalftone(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.queueBitmapRequest("/halftone/monochrome", params, userId));
    }

    @PostMapping("/preview/dither")
    public ResponseEntity<BitmapJobResponse> previewDither(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.queueBitmapRequest("/dither/", params, userId));
    }

    @PostMapping("/preview/separation-proof")
    public ResponseEntity<BitmapJobResponse> previewSeparationProof(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.queueBitmapRequest("/halftone/separation/proof", params, userId));
    }

    @PostMapping("/export/separation-zip")
    public ResponseEntity<BitmapJobResponse> exportSeparationZip(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.queueBitmapRequest("/halftone/separation", params, userId));
    }

    @PostMapping("/export/psd")
    public ResponseEntity<BitmapJobResponse> exportPsd(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.queueBitmapRequest("/halftone/separation/psd", params, userId));
    }

    @PostMapping("/export/cmyk")
    public ResponseEntity<BitmapJobResponse> exportCmyk(
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.queueBitmapRequest("/halftone/cmyk/", params, userId));
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<BitmapJobResponse> getJobStatus(
            @PathVariable Long jobId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.getBitmapJobStatus(jobId, userId));
    }

    @PostMapping(value = "/pattern/generate-seamless", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> generateSeamlessPattern(
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> params,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.generateSeamlessPattern(file, params, userId, authorizationHeader));
    }

    @PostMapping(value = "/gemini-image/image-to-image", consumes = "multipart/form-data")
    public ResponseEntity<GeminiImageToImageResponse> geminiImageToImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "mask_file", required = false) MultipartFile maskFile,
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(bitmapService.geminiImageToImage(file, maskFile, params, userId));
    }
}
