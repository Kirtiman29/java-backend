package com.rdc.admin.service;

import org.springframework.stereotype.Service;

@Service
public class SitemapService {

    private final String DOMAIN = "https://ruchitadesigncompany.in";

    // MAIN SITEMAP INDEX
    public String sitemapIndex() {

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">

            <sitemap>
                <loc>https://ruchitadesigncompany.in/sitemap-pages.xml</loc>
            </sitemap>

            <sitemap>
                <loc>https://ruchitadesigncompany.in/sitemap-categories.xml</loc>
            </sitemap>

            <sitemap>
                <loc>https://ruchitadesigncompany.in/sitemap-products.xml</loc>
            </sitemap>

        </sitemapindex>
        """;
    }

    // STATIC WEBSITE PAGES
    public String pagesSitemap() {

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">

        <url>
            <loc>https://ruchitadesigncompany.in/</loc>
        </url>

        <url>
            <loc>https://ruchitadesigncompany.in/about</loc>
        </url>

        <url>
            <loc>https://ruchitadesigncompany.in/contact</loc>
        </url>

        <url>
            <loc>https://ruchitadesigncompany.in/privacy-policy</loc>
        </url>

        </urlset>
        """;
    }

    // CATEGORY PAGES
    public String categorySitemap() {

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">

        <url>
            <loc>https://ruchitadesigncompany.in/categories</loc>
        </url>

        </urlset>
        """;
    }

    // PRODUCT / DESIGN PAGES
    public String productSitemap() {

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
        xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">

        <url>
            <loc>https://ruchitadesigncompany.in/design/sample-design</loc>

            <image:image>
                <image:loc>https://ruchitadesigncompany.in/images/sample.jpg</image:loc>
            </image:image>

        </url>

        </urlset>
        """;
    }

}