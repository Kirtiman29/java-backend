package com.rdc.admin.newsletter.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NewsletterSendResult {

    private Long campaignId;
    private int totalRecipients;
    private int successCount;
    private int failedCount;
}
