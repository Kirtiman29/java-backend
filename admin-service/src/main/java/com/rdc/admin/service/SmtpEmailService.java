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

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmtpEmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * ✅ UPDATED: Sends rich HTML emails using Thymeleaf templates.
     * Ensures the RDC branding (Segoe UI, #2A2623) is applied.
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Prepare Thymeleaf Context with variables for the template
            Context context = new Context();
            context.setVariables(variables);

            // Process the HTML template file from src/main/resources/templates/
            String htmlContent = templateEngine.process(templateName, context);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // Set to true for HTML rendering

            mailSender.send(message);
            log.info("HTML Recruitment email ({}) sent to: {}", templateName, to);
        } catch (Exception e) {
            log.error("Failed to send HTML recruitment email to {}: {}", to, e.getMessage());
        }
    }

    // Keep the simple method for basic logs if needed
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Simple mail failed: {}", e.getMessage());
        }
    }
}