package com.rdc.admin.controller;

import com.rdc.admin.service.SitemapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SitemapController {

    private final SitemapService sitemapService;

    @GetMapping(value="/sitemap.xml", produces="application/xml")
    public String sitemapIndex(){
        return sitemapService.sitemapIndex();
    }

    @GetMapping(value="/sitemap-products.xml", produces="application/xml")
    public String productSitemap(){
        return sitemapService.productSitemap();
    }

    @GetMapping(value="/sitemap-categories.xml", produces="application/xml")
    public String categorySitemap(){
        return sitemapService.categorySitemap();
    }

    @GetMapping(value="/sitemap-pages.xml", produces="application/xml")
    public String pagesSitemap(){
        return sitemapService.pagesSitemap();
    }

}