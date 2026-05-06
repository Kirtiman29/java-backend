package com.rdc.admin.newsletter.service;

import com.rdc.admin.newsletter.dto.NewsletterSendRequest;
import com.rdc.admin.newsletter.dto.NewsletterSendResult;
import com.rdc.admin.newsletter.entity.NewsletterCampaign;
import com.rdc.admin.newsletter.entity.NewsletterCampaignStatus;
import com.rdc.admin.newsletter.entity.NewsletterSubscriber;
import com.rdc.admin.newsletter.repository.NewsletterCampaignRepository;
import com.rdc.admin.newsletter.repository.NewsletterSubscriberRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsletterServiceTest {

    @Mock
    private NewsletterSubscriberRepository subscriberRepository;

    @Mock
    private NewsletterCampaignRepository campaignRepository;

    @Mock
    private NewsletterEmailTemplateService newsletterEmailTemplateService;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NewsletterService newsletterService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(newsletterService, "fromEmail", "newsletter@rdc.com");
        ReflectionTestUtils.setField(
                newsletterService,
                "unsubscribeUrlBase",
                "https://api.rdc.com/api/public/newsletter/unsubscribe"
        );
    }

    @Test
    void shouldCreateNewSubscriberWhenEmailDoesNotExist() {
        when(subscriberRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.empty());

        String message = newsletterService.subscribe(" User@Example.com ", " footer ");

        ArgumentCaptor<NewsletterSubscriber> captor = ArgumentCaptor.forClass(NewsletterSubscriber.class);
        verify(subscriberRepository).save(captor.capture());

        NewsletterSubscriber savedSubscriber = captor.getValue();
        assertEquals("Subscribed successfully.", message);
        assertEquals("user@example.com", savedSubscriber.getEmail());
        assertEquals("footer", savedSubscriber.getSource());
        assertTrue(savedSubscriber.isActive());
        assertNotNull(savedSubscriber.getSubscribedAt());
    }

    @Test
    void shouldReactivateInactiveSubscriber() {
        NewsletterSubscriber inactiveSubscriber = NewsletterSubscriber.builder()
                .id(2L)
                .email("user@example.com")
                .active(false)
                .subscribedAt(LocalDateTime.now().minusDays(10))
                .unsubscribedAt(LocalDateTime.now().minusDays(1))
                .source("blog")
                .build();

        when(subscriberRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(inactiveSubscriber));

        String message = newsletterService.subscribe("user@example.com", "homepage");

        verify(subscriberRepository).save(inactiveSubscriber);
        assertEquals("Subscribed successfully.", message);
        assertTrue(inactiveSubscriber.isActive());
        assertNull(inactiveSubscriber.getUnsubscribedAt());
        assertEquals("homepage", inactiveSubscriber.getSource());
    }

    @Test
    void shouldReturnAlreadySubscribedMessageForActiveSubscriber() {
        NewsletterSubscriber activeSubscriber = NewsletterSubscriber.builder()
                .id(3L)
                .email("user@example.com")
                .active(true)
                .subscribedAt(LocalDateTime.now())
                .build();

        when(subscriberRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(activeSubscriber));

        String message = newsletterService.subscribe("user@example.com", "footer");

        assertEquals("You are already subscribed.", message);
        verify(subscriberRepository, never()).save(any(NewsletterSubscriber.class));
    }

    @Test
    void shouldDeactivateSubscriberOnUnsubscribe() {
        NewsletterSubscriber activeSubscriber = NewsletterSubscriber.builder()
                .id(4L)
                .email("user@example.com")
                .active(true)
                .subscribedAt(LocalDateTime.now().minusDays(3))
                .build();

        when(subscriberRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(activeSubscriber));

        String message = newsletterService.unsubscribe("user@example.com");

        verify(subscriberRepository).save(activeSubscriber);
        assertEquals("You have been unsubscribed successfully.", message);
        assertTrue(!activeSubscriber.isActive());
        assertNotNull(activeSubscriber.getUnsubscribedAt());
    }

    @Test
    void shouldPersistCampaignAndCountMailFailures() {
        NewsletterSubscriber firstSubscriber = NewsletterSubscriber.builder()
                .id(1L)
                .email("first@example.com")
                .active(true)
                .subscribedAt(LocalDateTime.now().minusDays(2))
                .build();
        NewsletterSubscriber secondSubscriber = NewsletterSubscriber.builder()
                .id(2L)
                .email("second@example.com")
                .active(true)
                .subscribedAt(LocalDateTime.now().minusDays(1))
                .build();

        NewsletterSendRequest request = new NewsletterSendRequest();
        request.setSubject("New Design Collection Launched");
        request.setTitle("Fresh Textile Designs Are Live");
        request.setMessage("Explore our latest floral and abstract designs.");
        request.setButtonText("View Collection");
        request.setButtonUrl("https://ruchitadesigncompany.com/designs");

        when(subscriberRepository.findAllByActiveTrueOrderBySubscribedAtAsc())
                .thenReturn(List.of(firstSubscriber, secondSubscriber));
        when(campaignRepository.save(any(NewsletterCampaign.class)))
                .thenAnswer(invocation -> {
                    NewsletterCampaign campaign = invocation.getArgument(0);
                    if (campaign.getId() == null) {
                        campaign.setId(10L);
                    }
                    return campaign;
                });
        when(newsletterEmailTemplateService.buildNewsletterHtml(
                eq("Fresh Textile Designs Are Live"),
                eq("Explore our latest floral and abstract designs."),
                eq("View Collection"),
                eq("https://ruchitadesigncompany.com/designs"),
                any(String.class)
        )).thenReturn("<html>newsletter</html>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        AtomicInteger sendInvocationCount = new AtomicInteger();
        doAnswer(invocation -> {
            if (sendInvocationCount.getAndIncrement() == 1) {
                throw new MailSendException("SMTP failure");
            }
            return null;
        }).when(mailSender).send(any(MimeMessage.class));

        NewsletterSendResult result = newsletterService.sendNewsletter(request);

        assertEquals(10L, result.getCampaignId());
        assertEquals(2, result.getTotalRecipients());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailedCount());

        ArgumentCaptor<NewsletterCampaign> campaignCaptor = ArgumentCaptor.forClass(NewsletterCampaign.class);
        verify(campaignRepository, times(2)).save(campaignCaptor.capture());

        NewsletterCampaign initialCampaign = campaignCaptor.getAllValues().get(0);
        NewsletterCampaign completedCampaign = campaignCaptor.getAllValues().get(1);

        assertEquals(NewsletterCampaignStatus.SENDING, initialCampaign.getStatus());
        assertEquals(NewsletterCampaignStatus.PARTIAL_FAILED, completedCampaign.getStatus());
        assertEquals(2, completedCampaign.getTotalRecipients());
        assertEquals(1, completedCampaign.getSuccessCount());
        assertEquals(1, completedCampaign.getFailedCount());
        assertNotNull(completedCampaign.getSentAt());

        verify(newsletterEmailTemplateService, times(2)).buildNewsletterHtml(
                eq("Fresh Textile Designs Are Live"),
                eq("Explore our latest floral and abstract designs."),
                eq("View Collection"),
                eq("https://ruchitadesigncompany.com/designs"),
                any(String.class)
        );
    }
}
