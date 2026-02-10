package com.rdc.admin.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${rdc.hr.email}")
    private String adminEmail;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendContactMail(String name, String email, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Prepare Template Variables
            Context context = new Context();
            context.setVariable("customerName", name);
            context.setVariable("customerEmail", email);
            context.setVariable("customerMessage", message);

            // Process HTML template (matches the filename in step 2)
            String htmlContent = templateEngine.process("admin-contact-notification", context);

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("📩 New Industrial Inquiry: " + name);
            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Branded contact alert sent to admin for: {}", name);
        } catch (Exception e) {
            log.error("Failed to send branded contact email: {}", e.getMessage());
        }
    }
}