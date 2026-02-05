package com.rdc.order.service;

import com.rdc.order.entity.Order;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final InvoiceGeneratorService invoiceGeneratorService;

    @Async // ✅ Requires @EnableAsync in your Application class
    public void sendOrderConfirmation(Order order, String userEmail, String userName) {
        try {
            log.info("📧 Preparing order confirmation email for Order #{}", order.getId());

            Context context = new Context();
            context.setVariable("name", userName);
            context.setVariable("orderId", order.getId());
            context.setVariable("amount", order.getTotalPriceCents() / 100.0);
            context.setVariable("items", order.getItems()); // Pass items for email summary

            String htmlContent = templateEngine.process("order-success", context);

            // ✅ Fault-tolerant PDF generation
            byte[] invoicePdf = null;
            try {
                invoicePdf = invoiceGeneratorService.generateInvoicePdf(order);
            } catch (Exception e) {
                log.error("❌ PDF generation failed for Order #{}: {}", order.getId(), e.getMessage());
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("RDC Industrial Archive <yourmail@gmail.com>");
            helper.setTo(userEmail);
            helper.setSubject("Payment Successful | Invoice #" + order.getId());
            helper.setText(htmlContent, true);

            // ✅ Attach PDF only if it was successfully generated
            if (invoicePdf != null) {
                helper.addAttachment(
                        "Invoice-RDC-" + order.getId() + ".pdf",
                        new ByteArrayResource(invoicePdf)
                );
            }

            mailSender.send(message);
            log.info("✅ Invoice email sent for Order #{}", order.getId());

        } catch (Exception e) {
            log.error("❌ Email process failed for Order #{}: {}", order.getId(), e.getMessage());
        }
    }
}