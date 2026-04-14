package com.rdc.order.service;

import com.rdc.order.entity.Order;
import com.rdc.order.entity.OrderItem;
import com.rdc.order.repository.OrderItemRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // ✅ Added
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

    // ✅ FIXED: Injected from environment to avoid hardcoded email
    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOrderConfirmation(Order order, String userEmail, String userName) {
        log.info("📧 Preparing tax invoice for Order #{}", order.getId());

        try {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

            byte[] invoicePdf = invoiceGeneratorService.generateInvoicePdf(order, items);

            if (invoicePdf == null || invoicePdf.length == 0) {
                log.error("⚠️ Invoice PDF generated as empty for Order #{}", order.getId());
                return;
            }

            // Execute the actual sending logic
            sendEmailLogic(order, items, userEmail, userName, invoicePdf);

        } catch (Exception e) {
            log.error("❌ CRITICAL: Invoice preparation failed for Order #{}", order.getId(), e);
        }
    }

    /**
     * ✅ ENHANCEMENT: Logic separated for clarity.
     * Ensure your main Application class has @EnableAsync.
     */
    @Async
    public void sendEmailLogic(Order order, List<OrderItem> items, String userEmail, String userName, byte[] invoicePdf) {
        try {
            log.info("Initiating email delivery for Order #{}", order.getId());

            Context context = new Context();
            context.setVariable("name", userName);
            context.setVariable("orderId", order.getId());
            context.setVariable("totalAmount", order.getGrandTotalCents() / 100.0);
            context.setVariable("items", items);

            String htmlContent = templateEngine.process("order-success", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // ✅ FIXED: Uses dynamic fromEmail with a display name
            helper.setFrom("RDC Storefront <" + fromEmail + ">");
            helper.setTo(userEmail);
            helper.setSubject("Payment Success | Tax Invoice #" + order.getId());
            helper.setText(htmlContent, true);

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