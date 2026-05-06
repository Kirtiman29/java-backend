package com.rdc.admin.newsletter.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class NewsletterSendRequest {

    @NotBlank
    private String subject;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    private String buttonText;

    private String buttonUrl;

    @AssertTrue(message = "buttonText and buttonUrl must both be provided together.")
    public boolean isButtonConfigurationValid() {
        boolean hasButtonText = StringUtils.hasText(buttonText);
        boolean hasButtonUrl = StringUtils.hasText(buttonUrl);
        return hasButtonText == hasButtonUrl;
    }
}
