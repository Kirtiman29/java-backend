package com.rdc.admin.newsletter.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterCampaignResponse {

    private Long id;
    private String subject;
    private String title;
    private int totalRecipients;
    private int successCount;
    private int failedCount;
    private String status;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
