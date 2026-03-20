package com.rdc.admin.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SitemapService {

    private final String DOMAIN = "https://ruchitadesigncompany.in";

    private String today() {
        return LocalDate.now().toString();
    }

    // =========================
    // MAIN SITEMAP INDEX
    // =========================
    public String sitemapIndex() {

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">

            <sitemap>
                <loc>%s/sitemap-pages.xml</loc>
                <lastmod>%s</lastmod>
            </sitemap>

            <sitemap>
                <loc>%s/sitemap-categories.xml</loc>
                <lastmod>%s</lastmod>
            </sitemap>

            <sitemap>
                <loc>%s/sitemap-products.xml</loc>
                <lastmod>%s</lastmod>
            </sitemap>

        </sitemapindex>
        """.formatted(DOMAIN, today(), DOMAIN, today(), DOMAIN, today());
    }

    // =========================
    // STATIC WEBSITE PAGES
    // =========================
    public String pagesSitemap() {

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">

            <url>
                <loc>%s/</loc>
                <lastmod>%s</lastmod>
                <changefreq>daily</changefreq>
                <priority>1.0</priority>
            </url>

            <url>
                <loc>%s/about</loc>
                <lastmod>%s</lastmod>
                <changefreq>monthly</changefreq>
                <priority>0.8</priority>
            </url>

            <url>
                <loc>%s/gallery</loc>
                <lastmod>%s</lastmod>
                <changefreq>weekly</changefreq>
                <priority>0.9</priority>
            </url>

            <url>
                <loc>%s/luxury</loc>
                <lastmod>%s</lastmod>
                <changefreq>weekly</changefreq>
                <priority>0.8</priority>
            </url>

            <url>
                <loc>%s/trends</loc>
                <lastmod>%s</lastmod>
                <changefreq>weekly</changefreq>
                <priority>0.8</priority>
            </url>

            <url>
                <loc>%s/special-offers</loc>
                <lastmod>%s</lastmod>
                <changefreq>weekly</changefreq>
                <priority>0.7</priority>
            </url>

            <url>
                <loc>%s/contact</loc>
                <lastmod>%s</lastmod>
                <changefreq>yearly</changefreq>
                <priority>0.6</priority>
            </url>

            <url>
                <loc>%s/faq</loc>
                <lastmod>%s</lastmod>
                <changefreq>monthly</changefreq>
                <priority>0.6</priority>
            </url>

            <url>
                <loc>%s/terms</loc>
                <lastmod>%s</lastmod>
                <changefreq>yearly</changefreq>
                <priority>0.5</priority>
            </url>

            <url>
                <loc>%s/privacy</loc>
                <lastmod>%s</lastmod>
                <changefreq>yearly</changefreq>
                <priority>0.5</priority>
            </url>

        </urlset>
        """.formatted(
                DOMAIN, today(),
                DOMAIN, today(),
                DOMAIN, today(),
                DOMAIN, today(),
                DOMAIN, today(),
                DOMAIN, today(),
                DOMAIN, today(),
                DOMAIN, today(),
                DOMAIN, today(),
                DOMAIN, today()
        );
    }

    // =========================
    // CATEGORY PAGES
    // =========================
    public String categorySitemap() {

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">

            <url>
                <loc>%s/categories</loc>
                <lastmod>%s</lastmod>
                <changefreq>weekly</changefreq>
                <priority>0.9</priority>
            </url>

            <url>
                <loc>%s/categories/luxury</loc>
                <lastmod>%s</lastmod>
                <changefreq>weekly</changefreq>
                <priority>0.8</priority>
            </url>

            <url>
                <loc>%s/categories/trends</loc>
                <lastmod>%s</lastmod>
                <changefreq>weekly</changefreq>
                <priority>0.8</priority>
            </url>

        </urlset>
        """.formatted(
                DOMAIN, today(),
                DOMAIN, today(),
                DOMAIN, today()
        );
    }

    // =========================
    // PRODUCT / DESIGN PAGES
    // =========================
    public String productSitemap() {

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">

            <url>
                <loc>%s/design/sample-design</loc>
                <lastmod>%s</lastmod>
                <changefreq>weekly</changefreq>
                <priority>0.9</priority>

                <image:image>
                    <image:loc>%s/images/sample.jpg</image:loc>
                </image:image>
            </url>

        </urlset>
        """.formatted(DOMAIN, today(), DOMAIN);
    }
}