package com.rdc.admin.controller;

import com.rdc.admin.service.SitemapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final SitemapService sitemapService;

    // Common headers (SEO + performance)
    private HttpHeaders xmlHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);

        // Cache for 6 hours (Google bots love caching)
        headers.setCacheControl("public, max-age=21600");

        return headers;
    }

    // =========================
    // MAIN SITEMAP INDEX
    // =========================
    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> sitemapIndex() {
        return ResponseEntity
                .ok()
                .headers(xmlHeaders())
                .body(sitemapService.sitemapIndex());
    }

    // =========================
    // PRODUCT SITEMAP
    // =========================
    @GetMapping("/sitemap-products.xml")
    public ResponseEntity<String> productSitemap() {
        return ResponseEntity
                .ok()
                .headers(xmlHeaders())
                .body(sitemapService.productSitemap());
    }

    // =========================
    // CATEGORY SITEMAP
    // =========================
    @GetMapping("/sitemap-categories.xml")
    public ResponseEntity<String> categorySitemap() {
        return ResponseEntity
                .ok()
                .headers(xmlHeaders())
                .body(sitemapService.categorySitemap());
    }

    // =========================
    // STATIC PAGES SITEMAP
    // =========================
    @GetMapping("/sitemap-pages.xml")
    public ResponseEntity<String> pagesSitemap() {
        return ResponseEntity
                .ok()
                .headers(xmlHeaders())
                .body(sitemapService.pagesSitemap());
    }
}