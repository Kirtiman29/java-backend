package com.rdc.subscription.controller;

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
@RequestMapping("/api")
public class TraditionalArtController {

    private final BitmapService bitmapService;

    @PostMapping(value = "/traditional-art", consumes = "multipart/form-data")
    public ResponseEntity<Map> traditionalArt(
            @RequestParam("image") MultipartFile image,
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bitmapService.traditionalArt(image, params, userId);
    }

    @PostMapping(value = "/traditional-art-preview", consumes = "multipart/form-data")
    public ResponseEntity<Map> traditionalArtPreview(
            @RequestParam("image") MultipartFile image,
            @RequestParam Map<String, String> params,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bitmapService.traditionalArt(image, params, userId);
    }
}
