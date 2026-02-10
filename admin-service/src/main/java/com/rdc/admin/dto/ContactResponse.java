package com.rdc.admin.dto;

import com.rdc.admin.entity.ContactMessage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ContactResponse {

    private Long id;
    private String name;
    private String email;
    private String message;
    private ContactMessage.Status status;
    private LocalDateTime createdAt;
}
