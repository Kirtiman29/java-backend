package com.rdc.admin.newsletter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "newsletter_campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "button_text", length = 100)
    private String buttonText;

    @Column(name = "button_url", length = 500)
    private String buttonUrl;

    @Builder.Default
    @Column(name = "total_recipients", nullable = false)
    private int totalRecipients = 0;

    @Builder.Default
    @Column(name = "success_count", nullable = false)
    private int successCount = 0;

    @Builder.Default
    @Column(name = "failed_count", nullable = false)
    private int failedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NewsletterCampaignStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
