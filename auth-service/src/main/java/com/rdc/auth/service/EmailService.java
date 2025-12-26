package com.rdc.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine; // Injected Thymeleaf Engine

    @Value("${app.email.sender}")
    private String fromEmail;

    // Constructor Injection
    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Generic method to send a templated HTML email.
     */
    private void sendHtmlEmail(String toEmail, String subject, String templateName, Context context) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);

            // Process the HTML template
            String htmlContent = templateEngine.process(templateName, context);

            // Set the email content as HTML (the 'true' flag here is CRITICAL)
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("HTML email sent successfully to: " + toEmail + " with subject: " + subject);
        } catch (Exception e) {
            System.err.println("Error sending HTML email to " + toEmail + " [" + subject + "]: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sends the account verification email (HTML).
     */
    public void sendVerificationEmail(String toEmail, String verificationUrl) {
        String subject = "RDC Account Verification Required";

        Context context = new Context();
        context.setVariable("verificationLink", verificationUrl);

        sendHtmlEmail(toEmail, subject, "verification-email", context); // Uses verification-email.html
    }

    /**
     * Sends the password reset email (HTML).
     */
    public void sendResetPasswordEmail(String toEmail, String resetUrl) {
        String subject = "RDC Account Password Reset";

        Context context = new Context();
        context.setVariable("resetLink", resetUrl);

        sendHtmlEmail(toEmail, subject, "password-reset-email", context); // Uses password-reset-email.html
    }
}