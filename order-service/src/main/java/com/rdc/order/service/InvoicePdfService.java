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

    public byte[] generatePdf(Order order, List<OrderItem> items) {
        log.info("📄 Generating PDF for Order #{} with {} items", order.getId(), items.size());

        String html = generateInvoiceHtml(order, items);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("❌ PDF Engine Error: {}", e.getMessage());
            throw new RuntimeException("Invoice PDF generation failed", e);
        }
    }

    public String generateInvoiceHtml(Order order, List<OrderItem> items) {
        Context context = new Context();

        context.setVariable("order", order);
        context.setVariable("items", items);
        context.setVariable("invoiceNumber", "RDC-" + order.getId());
        context.setVariable("orderDate", formatInstant(order.getCreatedAt()));
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