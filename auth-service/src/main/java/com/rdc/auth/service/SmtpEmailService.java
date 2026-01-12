package com.rdc.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
@RequiredArgsConstructor // Automatically generates constructor for 'final' fields
@Slf4j
public class SmtpEmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.email.sender}")
    private String fromEmail;

    public void sendVerificationEmail(String toEmail, String verificationUrl) {
        log.info("Sending verification email to: {}", toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify your RDC Account");

            String htmlContent;
            try {
                Context context = new Context();
                context.setVariable("verificationLink", verificationUrl);
                htmlContent = templateEngine.process("verification-email", context);
            } catch (Exception e) {
                log.warn("Template error, using fallback HTML: {}", e.getMessage());
                htmlContent = buildVerificationEmailHtml(verificationUrl);
            }

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("SMTP Error: {}", e.getMessage());
            throw new RuntimeException("Unable to send verification email.", e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        log.info("Sending password reset email to: {}", toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset your RDC Password");

            String htmlContent;
            try {
                Context context = new Context();
                context.setVariable("resetLink", resetUrl);
                htmlContent = templateEngine.process("password-reset-email", context);
            } catch (Exception e) {
                log.warn("Template error, using fallback HTML: {}", e.getMessage());
                htmlContent = buildPasswordResetEmailHtml(resetUrl);
            }

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("SMTP Error: {}", e.getMessage());
            throw new RuntimeException("Unable to send password reset email.", e);
        }
    }

    private String buildVerificationEmailHtml(String url) {
        return "<html><body><a href=\"" + url + "\">Verify Email</a></body></html>";
    }

    private String buildPasswordResetEmailHtml(String url) {
        return "<html><body><a href=\"" + url + "\">Reset Password</a></body></html>";
    }
}
