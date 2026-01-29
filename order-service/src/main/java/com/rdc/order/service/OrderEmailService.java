package com.rdc.order.service;

import com.rdc.order.entity.Order;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Async // Use @EnableAsync in your Main class to send emails without blocking the request
    public void sendOrderConfirmation(Order order, String userEmail, String userName) {
        log.info("📧 Preparing confirmation email for Order #{} to {}", order.getId(), userEmail);

        try {
            Context context = new Context();
            context.setVariable("name", userName);
            context.setVariable("orderId", order.getId());
            context.setVariable("amount", order.getTotalPriceCents() / 100.0);
            context.setVariable("items", order.getItems());

            String htmlContent = templateEngine.process("order-success", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("RDC Industrial Archive <yourmail@gmail.com>");
            helper.setTo(userEmail);
            helper.setSubject("Payment Successful | Order #" + order.getId());
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("✅ Email sent successfully for Order #{}", order.getId());
        } catch (Exception e) {
            log.error("❌ Failed to send email for Order #{}: {}", order.getId(), e.getMessage());
        }
    }
}