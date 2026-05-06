package com.rdc.admin.newsletter.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class NewsletterEmailTemplateService {

    public String buildNewsletterHtml(
            String title,
            String message,
            String buttonText,
            String buttonUrl,
            String unsubscribeUrl
    ) {
        String safeTitle = HtmlUtils.htmlEscape(title);
        String safeMessage = HtmlUtils.htmlEscape(message)
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "<br/>");
        String safeUnsubscribeUrl = HtmlUtils.htmlEscape(unsubscribeUrl);

        String buttonHtml = "";
        if (StringUtils.hasText(buttonText) && StringUtils.hasText(buttonUrl)) {
            buttonHtml = """
                    <div style="margin:30px 0;">
                      <a href="%s" style="display:inline-block; padding:14px 26px; background:#111111; color:#ffffff; text-decoration:none; border-radius:999px; font-weight:600;">
                        %s
                      </a>
                    </div>
                    """.formatted(HtmlUtils.htmlEscape(buttonUrl), HtmlUtils.htmlEscape(buttonText));
        }

        return """
                <div style="font-family:Arial,sans-serif; background:#f7f7f7; padding:30px;">
                  <div style="max-width:600px; margin:auto; background:#ffffff; padding:30px; border-radius:12px; border:1px solid #ececec;">
                    <p style="margin:0 0 12px; font-size:13px; letter-spacing:2px; color:#7a7a7a; text-transform:uppercase;">RDC</p>
                    <h2 style="margin:0 0 20px; color:#111111;">Ruchita Design Company</h2>
                    <h1 style="margin:0 0 18px; color:#222222; font-size:28px; line-height:1.3;">%s</h1>
                    <p style="margin:0; font-size:16px; color:#555555; line-height:1.7;">%s</p>
                    %s
                    <hr style="margin:30px 0; border:none; border-top:1px solid #ececec;" />
                    <p style="margin:0 0 10px; font-size:12px; color:#999999;">
                      You received this email because you subscribed to RDC updates.
                    </p>
                    <p style="margin:0; font-size:12px;">
                      <a href="%s" style="color:#666666;">Unsubscribe</a>
                    </p>
                  </div>
                </div>
                """.formatted(safeTitle, safeMessage, buttonHtml, safeUnsubscribeUrl);
    }
}
