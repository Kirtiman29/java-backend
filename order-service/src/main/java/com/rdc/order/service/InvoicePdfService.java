package com.rdc.order.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private final SpringTemplateEngine templateEngine;
    private final InvoiceAssetService invoiceAssetService;

    /**
     * ✅ UPDATED: Now accepts pre-fetched items list.
     * Prevents LazyInitializationException and Hibernate collection assignment errors.
     */
    public byte[] generatePdf(Order order, List<OrderItem> items) {
        log.info("📄 Generating PDF for Order #{} with {} items", order.getId(), items.size());

        String html = generateInvoiceHtml(order, items);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode(); // Performance boost for high-volume invoice generation
            builder.withHtmlContent(html, "");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("❌ PDF Engine Error: {}", e.getMessage());
            throw new RuntimeException("Invoice PDF generation failed", e);
        }
    }

    /**
     * ✅ UPDATED: Processes the HTML template using the explicit items list.
     * Direct variable passing ensures designIdentifier is visible to Thymeleaf.
     */
    public String generateInvoiceHtml(Order order, List<OrderItem> items) {
        Context context = new Context();

        // Pass essential data to the Thymeleaf template [cite: 368]
        context.setVariable("order", order);
        context.setVariable("items", items); // Direct list usage [cite: 368, 484]
        context.setVariable("invoiceNumber", "RDC-" + order.getId());
        context.setVariable("orderDate", formatInstant(order.getCreatedAt()));
        context.setVariable("logoBase64", invoiceAssetService.getLogoBase64());

        return templateEngine.process("invoice", context);
    }

    private String formatInstant(Instant instant) {
        if (instant == null) return "N/A";
        return DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.of("Asia/Kolkata")) // Standardized for RDC Mumbai headquarters
                .format(instant);
    }
}