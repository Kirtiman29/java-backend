package com.rdc.admin.newsletter.controller;

import com.rdc.admin.newsletter.dto.NewsletterCampaignResponse;
import com.rdc.admin.newsletter.dto.NewsletterSendRequest;
import com.rdc.admin.newsletter.dto.NewsletterSendResult;
import com.rdc.admin.newsletter.dto.NewsletterSubscriberResponse;
import com.rdc.admin.newsletter.service.NewsletterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/newsletter")
public class AdminNewsletterController {

    private final NewsletterService newsletterService;

    @GetMapping("/subscribers")
    public ResponseEntity<List<NewsletterSubscriberResponse>> getSubscribers() {
        return ResponseEntity.ok(newsletterService.getSubscribers());
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendNewsletter(@Valid @RequestBody NewsletterSendRequest request) {
        NewsletterSendResult result = newsletterService.sendNewsletter(request);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "campaignId", result.getCampaignId(),
                "totalRecipients", result.getTotalRecipients(),
                "successCount", result.getSuccessCount(),
                "failedCount", result.getFailedCount(),
                "message", "Newsletter sent successfully."
        ));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<NewsletterCampaignResponse>> getCampaigns() {
        return ResponseEntity.ok(newsletterService.getCampaigns());
    }
}
