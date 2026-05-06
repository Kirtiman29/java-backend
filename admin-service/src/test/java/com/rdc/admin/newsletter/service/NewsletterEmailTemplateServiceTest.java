package com.rdc.admin.newsletter.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewsletterEmailTemplateServiceTest {

    private final NewsletterEmailTemplateService service = new NewsletterEmailTemplateService();

    @Test
    void shouldIncludeButtonAndEscapeDynamicContent() {
        String html = service.buildNewsletterHtml(
                "Fresh <Launch>",
                "Line one\nLine two",
                "View Collection",
                "https://ruchitadesigncompany.com/designs?ref=mail",
                "https://api.rdc.com/api/public/newsletter/unsubscribe?email=user%40example.com"
        );

        assertTrue(html.contains("Fresh &lt;Launch&gt;"));
        assertTrue(html.contains("Line one<br/>Line two"));
        assertTrue(html.contains("View Collection"));
        assertTrue(html.contains("https://ruchitadesigncompany.com/designs?ref=mail"));
        assertTrue(html.contains("Unsubscribe"));
    }

    @Test
    void shouldOmitButtonMarkupWhenButtonIsNotProvided() {
        String html = service.buildNewsletterHtml(
                "Update",
                "Body",
                null,
                null,
                "https://api.rdc.com/api/public/newsletter/unsubscribe?email=user%40example.com"
        );

        assertFalse(html.contains("View Collection"));
        assertFalse(html.contains("background:#111111"));
    }
}
