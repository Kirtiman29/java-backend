package com.rdc.order.service;

import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.repository.OrderItemRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final InvoiceGeneratorService invoiceGeneratorService;
    private final OrderItemRepository orderItemRepository;

    /**
     * ✅ STEP 1: Main logic (Synchronous)
     * FIXED: Removed order.setItems(items) to prevent Hibernate Orphan Deletion error.
     */
    public void sendOrderConfirmation(Order order, String userEmail, String userName) {
        log.info("📧 Preparing tax invoice for Order #{}", order.getId());

        try {
            // 1️⃣ Fetch items explicitly to ensure designIdentifiers (SKU) are loaded
            // We use a local variable to avoid messing with Hibernate's managed collection [cite: 376]
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

            // 2️⃣ Generate the PDF byte array synchronously
            // Pass the local items list explicitly to ensure SKU visibility in the PDF
            byte[] invoicePdf = invoiceGeneratorService.generateInvoicePdf(order, items);

            if (invoicePdf == null || invoicePdf.length == 0) {
                log.error("⚠️ Invoice PDF generated as empty for Order #{}", order.getId());
                return;
            }

            // 3️⃣ Pass the local items and PDF bytes to the async mail sender
            // Explicitly passing 'items' ensures the async thread has all metadata [cite: 379]
            sendEmailAsync(order, items, userEmail, userName, invoicePdf);

        } catch (Exception e) {
            log.error("❌ CRITICAL: Invoice preparation failed for Order #{}", order.getId(), e);
        }
    }

    /**
     * ✅ STEP 2: Async Mail Sender
     * FIXED: Receiving List<OrderItem> directly to ensure designIdentifier visibility
     * even if the main database session is closed.
     */
    @Async
    protected void sendEmailAsync(Order order, List<OrderItem> items, String userEmail, String userName, byte[] invoicePdf) {
        try {
            log.info("🚀 Initiating async email delivery for Order #{}", order.getId());

            Context context = new Context();
            context.setVariable("name", userName);
            context.setVariable("orderId", order.getId());

            // Accurate inclusive GST total for display [cite: 384]
            context.setVariable("totalAmount", order.getTotalPriceCents() / 100.0);

            // ✅ Use the passed items list directly for the email template
            // This ensures 'designIdentifier' is available for the 'order-success' template [cite: 385]
            context.setVariable("items", items);

            String htmlContent = templateEngine.process("order-success", context);

            MimeMessage message = mailSender.createMimeMessage();
            // 'true' indicates multipart message for attachment [cite: 386]
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("RDC Storefront <mail.ruchitadesigncompany@gmail.com>");
            helper.setTo(userEmail);
            helper.setSubject("Payment Success | Tax Invoice #" + order.getId());
            helper.setText(htmlContent, true);

            // Attach the PDF from the byte array prepared in the main thread [cite: 388]
            helper.addAttachment(
                    "Invoice-RDC-" + order.getId() + ".pdf",
                    new ByteArrayResource(invoicePdf)
            );

            mailSender.send(message);
            log.info("✅ Invoice email successfully SENT to {}", userEmail);

        } catch (Exception e) {
            log.error("❌ SMTP Error: Failed to send email for Order #{}", order.getId(), e);
        }
    }
}