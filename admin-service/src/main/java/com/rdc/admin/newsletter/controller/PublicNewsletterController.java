package com.rdc.admin.newsletter.controller;

import com.rdc.admin.newsletter.dto.NewsletterSubscribeRequest;
import com.rdc.admin.newsletter.service.NewsletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/newsletter")
public class PublicNewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(@Valid @RequestBody NewsletterSubscribeRequest request) {
        String message = newsletterService.subscribe(request.getEmail(), request.getSource());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message
        ));
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(@RequestParam String email) {
        String message = newsletterService.unsubscribe(email);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", message
        ));
    }
}
