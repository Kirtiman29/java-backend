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
public class NewsletterSubscriberResponse {

    private Long id;
    private String email;
    private boolean active;
    private LocalDateTime subscribedAt;
    private LocalDateTime unsubscribedAt;
    private String source;
}
