package com.rdc.admin.newsletter.service;

import com.rdc.admin.newsletter.dto.NewsletterCampaignResponse;
import com.rdc.admin.newsletter.dto.NewsletterSendRequest;
import com.rdc.admin.newsletter.dto.NewsletterSendResult;
import com.rdc.admin.newsletter.dto.NewsletterSubscriberResponse;
import com.rdc.admin.newsletter.entity.NewsletterCampaign;
import com.rdc.admin.newsletter.entity.NewsletterCampaignStatus;
import com.rdc.admin.newsletter.entity.NewsletterSubscriber;
import com.rdc.admin.newsletter.repository.NewsletterCampaignRepository;
import com.rdc.admin.newsletter.repository.NewsletterSubscriberRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepository;
    private final NewsletterCampaignRepository campaignRepository;
    private final NewsletterEmailTemplateService newsletterEmailTemplateService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${rdc.newsletter.unsubscribe-url-base:http://localhost:8080/api/public/newsletter/unsubscribe}")
    private String unsubscribeUrlBase;

    @Transactional
    public String subscribe(String email, String source) {
        String normalizedEmail = normalizeEmail(email);
        String normalizedSource = normalizeSource(source);
        LocalDateTime now = LocalDateTime.now();

        return subscriberRepository.findByEmailIgnoreCase(normalizedEmail)
                .map(existingSubscriber -> handleExistingSubscriber(existingSubscriber, normalizedSource, now))
                .orElseGet(() -> createSubscriber(normalizedEmail, normalizedSource, now));
    }

    @Transactional
    public String unsubscribe(String email) {
        String normalizedEmail = normalizeEmail(email);

        subscriberRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(subscriber -> {
            subscriber.setActive(false);
            subscriber.setUnsubscribedAt(LocalDateTime.now());
            subscriberRepository.save(subscriber);
        });

        return "You have been unsubscribed successfully.";
    }

    @Transactional(readOnly = true)
    public List<NewsletterSubscriberResponse> getSubscribers() {
        return subscriberRepository.findAllByOrderBySubscribedAtDesc().stream()
                .map(this::mapSubscriberResponse)
                .toList();
    }

    @Transactional
    public NewsletterSendResult sendNewsletter(NewsletterSendRequest request) {
        List<NewsletterSubscriber> subscribers = subscriberRepository.findAllByActiveTrueOrderBySubscribedAtAsc();

        NewsletterCampaign campaign = campaignRepository.save(NewsletterCampaign.builder()
                .subject(request.getSubject().trim())
                .title(request.getTitle().trim())
                .message(request.getMessage().trim())
                .buttonText(trimToNull(request.getButtonText()))
                .buttonUrl(trimToNull(request.getButtonUrl()))
                .status(NewsletterCampaignStatus.SENDING)
                .createdAt(LocalDateTime.now())
                .build());

        int successCount = 0;
        int failedCount = 0;

        for (NewsletterSubscriber subscriber : subscribers) {
            String unsubscribeUrl = buildUnsubscribeUrl(subscriber.getEmail());
            String html = newsletterEmailTemplateService.buildNewsletterHtml(
                    campaign.getTitle(),
                    campaign.getMessage(),
                    campaign.getButtonText(),
                    campaign.getButtonUrl(),
                    unsubscribeUrl
            );

            try {
                sendHtmlEmail(subscriber.getEmail(), campaign.getSubject(), html);
                successCount++;
            } catch (Exception ex) {
                failedCount++;
                log.error("Failed to send newsletter campaign {} to {}: {}", campaign.getId(), subscriber.getEmail(), ex.getMessage());
            }
        }

        campaign.setTotalRecipients(subscribers.size());
        campaign.setSuccessCount(successCount);
        campaign.setFailedCount(failedCount);
        campaign.setStatus(failedCount == 0 ? NewsletterCampaignStatus.SENT : NewsletterCampaignStatus.PARTIAL_FAILED);
        campaign.setSentAt(LocalDateTime.now());
        campaignRepository.save(campaign);

        return new NewsletterSendResult(
                campaign.getId(),
                subscribers.size(),
                successCount,
                failedCount
        );
    }

    @Transactional(readOnly = true)
    public List<NewsletterCampaignResponse> getCampaigns() {
        return campaignRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapCampaignResponse)
                .toList();
    }

    private String handleExistingSubscriber(NewsletterSubscriber subscriber, String source, LocalDateTime now) {
        if (subscriber.isActive()) {
            return "You are already subscribed.";
        }

        subscriber.setActive(true);
        subscriber.setSubscribedAt(now);
        subscriber.setUnsubscribedAt(null);
        subscriber.setSource(source);
        subscriberRepository.save(subscriber);

        return "Subscribed successfully.";
    }

    private String createSubscriber(String email, String source, LocalDateTime now) {
        NewsletterSubscriber subscriber = NewsletterSubscriber.builder()
                .email(email)
                .active(true)
                .subscribedAt(now)
                .source(source)
                .build();

        subscriberRepository.save(subscriber);
        return "Subscribed successfully.";
    }

    private NewsletterSubscriberResponse mapSubscriberResponse(NewsletterSubscriber subscriber) {
        return NewsletterSubscriberResponse.builder()
                .id(subscriber.getId())
                .email(subscriber.getEmail())
                .active(subscriber.isActive())
                .subscribedAt(subscriber.getSubscribedAt())
                .unsubscribedAt(subscriber.getUnsubscribedAt())
                .source(subscriber.getSource())
                .build();
    }

    private NewsletterCampaignResponse mapCampaignResponse(NewsletterCampaign campaign) {
        return NewsletterCampaignResponse.builder()
                .id(campaign.getId())
                .subject(campaign.getSubject())
                .title(campaign.getTitle())
                .totalRecipients(campaign.getTotalRecipients())
                .successCount(campaign.getSuccessCount())
                .failedCount(campaign.getFailedCount())
                .status(campaign.getStatus().name())
                .sentAt(campaign.getSentAt())
                .createdAt(campaign.getCreatedAt())
                .build();
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    private String buildUnsubscribeUrl(String email) {
        return UriComponentsBuilder.fromUriString(unsubscribeUrlBase)
                .queryParam("email", email)
                .build()
                .encode()
                .toUriString();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeSource(String source) {
        return trimToNull(source);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
