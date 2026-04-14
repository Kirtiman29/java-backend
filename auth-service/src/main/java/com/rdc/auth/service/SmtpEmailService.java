package com.rdc.auth.service;

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
public class SmtpEmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.email.sender}")
    private String fromEmail;

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            mailSender.send(message);
            log.info("Recruitment email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send recruitment email: {}", e.getMessage());
        }
    }

    public boolean sendVerificationEmail(String toEmail, String verificationUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("verificationLink", verificationUrl);

            String htmlContent = templateEngine.process("verification-email", context);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Verify your RDC Account");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML verification email sent to: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to send HTML email: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendPasswordResetEmail(String toEmail, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("resetLink", resetUrl);

            String htmlContent = templateEngine.process("password-reset-email", context);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Reset your RDC Password");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send reset email: {}", e.getMessage());
            return false;
        }
    }

    public boolean sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariable("otp", otp);

            String htmlContent = templateEngine.process("otp-email", context);

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Your OTP for Login");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
            return false;
        }
    }
}