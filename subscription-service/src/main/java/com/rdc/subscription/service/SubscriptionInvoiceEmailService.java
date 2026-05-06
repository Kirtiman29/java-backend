package com.rdc.subscription.service;

import com.rdc.subscription.entity.Plan;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionInvoiceEmailService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendSubscriptionInvoice(String toEmail, String customerName, Plan plan, LocalDateTime activatedAt) {
        if (fromEmail == null || fromEmail.isBlank()) {
            log.info("Skipping subscription invoice email because spring.mail.username is not configured");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("RDC Storefront <" + fromEmail + ">");
            helper.setTo(toEmail);
            helper.setSubject("RDC Subscription Invoice | " + plan.getName());
            helper.setText(buildHtml(customerName, plan, activatedAt), true);

            mailSender.send(message);
            log.info("Subscription invoice email sent to {} for plan {}", toEmail, plan.getName());
        } catch (Exception ex) {
            log.error("Failed to send subscription invoice to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }

    private String buildHtml(String customerName, Plan plan, LocalDateTime activatedAt) {
        String safeName = HtmlUtils.htmlEscape(customerName == null || customerName.isBlank() ? "Customer" : customerName);
        String safePlanName = HtmlUtils.htmlEscape(plan.getName());
        String safePlanType = HtmlUtils.htmlEscape(plan.getPlanType().name());
        String safeBillingCycle = HtmlUtils.htmlEscape(plan.getBillingCycle().name());
        String formattedPrice = formatCurrency(plan.getPrice().doubleValue());
        String formattedDate = activatedAt.format(DATE_FORMATTER);

        return """
                <div style="font-family:Arial,sans-serif; background:#f4f1ea; padding:32px;">
                  <div style="max-width:680px; margin:0 auto; background:#ffffff; border-radius:18px; overflow:hidden; border:1px solid #e8dfd1;">
                    <div style="background:linear-gradient(135deg,#111111,#4a3728); color:#ffffff; padding:28px 32px;">
                      <div style="font-size:12px; letter-spacing:2px; text-transform:uppercase; opacity:0.85;">Ruchita Design Company</div>
                      <h1 style="margin:12px 0 6px; font-size:28px;">Subscription Invoice</h1>
                      <p style="margin:0; font-size:14px; opacity:0.88;">Your subscription has been activated successfully.</p>
                    </div>
                    <div style="padding:32px;">
                      <p style="margin:0 0 20px; font-size:16px; color:#333333;">Hello %s,</p>
                      <p style="margin:0 0 24px; font-size:15px; line-height:1.7; color:#555555;">
                        Thank you for subscribing to RDC. Your plan is now active and the invoice summary is below.
                      </p>
                      <div style="border:1px solid #ece5da; border-radius:14px; overflow:hidden;">
                        <div style="display:flex; justify-content:space-between; gap:20px; padding:16px 20px; background:#faf7f2; border-bottom:1px solid #ece5da;">
                          <strong style="color:#2b241d;">Plan</strong>
                          <span style="color:#2b241d;">%s</span>
                        </div>
                        <div style="display:flex; justify-content:space-between; gap:20px; padding:16px 20px; border-bottom:1px solid #ece5da;">
                          <strong style="color:#2b241d;">Plan Type</strong>
                          <span style="color:#5c5044;">%s</span>
                        </div>
                        <div style="display:flex; justify-content:space-between; gap:20px; padding:16px 20px; border-bottom:1px solid #ece5da;">
                          <strong style="color:#2b241d;">Billing Cycle</strong>
                          <span style="color:#5c5044;">%s</span>
                        </div>
                        <div style="display:flex; justify-content:space-between; gap:20px; padding:16px 20px; border-bottom:1px solid #ece5da;">
                          <strong style="color:#2b241d;">Design Limit</strong>
                          <span style="color:#5c5044;">%d</span>
                        </div>
                        <div style="display:flex; justify-content:space-between; gap:20px; padding:16px 20px; border-bottom:1px solid #ece5da;">
                          <strong style="color:#2b241d;">AI Credits</strong>
                          <span style="color:#5c5044;">%d</span>
                        </div>
                        <div style="display:flex; justify-content:space-between; gap:20px; padding:16px 20px; border-bottom:1px solid #ece5da;">
                          <strong style="color:#2b241d;">Activated On</strong>
                          <span style="color:#5c5044;">%s</span>
                        </div>
                        <div style="display:flex; justify-content:space-between; gap:20px; padding:18px 20px; background:#111111;">
                          <strong style="color:#ffffff;">Amount Paid</strong>
                          <span style="color:#ffffff; font-size:18px; font-weight:700;">%s</span>
                        </div>
                      </div>
                      <p style="margin:24px 0 0; font-size:13px; line-height:1.7; color:#7a6c5f;">
                        This email serves as your subscription invoice confirmation. Please keep it for your records.
                      </p>
                    </div>
                  </div>
                </div>
                """.formatted(
                safeName,
                safePlanName,
                safePlanType,
                safeBillingCycle,
                plan.getDesignLimit(),
                plan.getCreditLimit(),
                HtmlUtils.htmlEscape(formattedDate),
                HtmlUtils.htmlEscape(formattedPrice)
        );
    }

    private String formatCurrency(double amount) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return format.format(amount);
    }
}
