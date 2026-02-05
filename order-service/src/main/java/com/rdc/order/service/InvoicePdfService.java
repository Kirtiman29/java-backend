package com.rdc.order.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
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
public class InvoicePdfService {

    private final SpringTemplateEngine templateEngine;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceAssetService invoiceAssetService;

    /**
     * ✅ UPDATED: Added BaseUri resolution
     * Even with Base64, openhtmltopdf works best when a baseUri is provided
     * to prevent security or resolution exceptions.
     */
    public byte[] generatePdf(Order order) {
        String html = generateInvoiceHtml(order);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            // Set BaseUri to an empty string or a valid URL to allow Base64 processing
            builder.withHtmlContent(html, "");
            builder.toStream(os);
            builder.run();

            return os.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Invoice PDF generation failed", e);
        }
    }

    public String generateInvoiceHtml(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        Context context = new Context();
        context.setVariable("order", order);
        context.setVariable("items", items);
        context.setVariable("invoiceNumber", "RDC-" + order.getId());
        context.setVariable("orderDate", formatInstant(order.getCreatedAt()));

        // ✅ Ensure this returns the raw Base64 string from src/main/resources/static/logo.png
        context.setVariable("logoBase64", invoiceAssetService.getLogoBase64());

        return templateEngine.process("invoice", context);
    }

    private String formatInstant(Instant instant) {
        if (instant == null) return "N/A";
        return DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .withZone(ZoneId.of("Asia/Kolkata"))
                .format(instant);
    }
}