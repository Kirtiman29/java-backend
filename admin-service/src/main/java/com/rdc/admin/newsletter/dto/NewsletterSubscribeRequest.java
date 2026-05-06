package com.rdc.admin.newsletter.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsletterSubscribeRequest {

    @Email
    @NotBlank
    private String email;

    private String source;
}
