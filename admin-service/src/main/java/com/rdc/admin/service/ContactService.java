package com.rdc.admin.service;

import com.rdc.admin.dto.ContactRequest;
import com.rdc.admin.dto.ContactResponse;
import com.rdc.admin.entity.ContactMessage;
import com.rdc.admin.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactMessageRepository repository;
    private final MailService mailService;

    /**
     * Saves a new message and sends an internal notification email.
     */
    @Transactional
    public void submit(ContactRequest request) {
        ContactMessage message = ContactMessage.builder()
                .name(request.getName())
                .email(request.getEmail())
                .message(request.getMessage())
                .status(ContactMessage.Status.NEW)
                .build();

        repository.save(message);

        try {
            mailService.sendContactMail(request.getName(), request.getEmail(), request.getMessage());
        } catch (Exception e) {
            log.error("Failed to send contact notification email: {}", e.getMessage());
        }
    }

    /**
     * Retrieves all messages, sorted by newest first.
     */
    @Transactional(readOnly = true)
    public List<ContactResponse> getAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Finds a single message for status verification or detail view.
     */
    @Transactional(readOnly = true)
    public ContactResponse getById(Long id) {
        ContactMessage message = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found with ID: " + id));
        return mapToResponse(message);
    }

    /**
     * Manually updates the lifecycle status of a message.
     */
    @Transactional
    public void updateStatus(Long id, ContactMessage.Status status) {
        ContactMessage msg = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact message not found with ID: " + id));
        msg.setStatus(status);
        repository.save(msg);
    }

    private ContactResponse mapToResponse(ContactMessage m) {
        return ContactResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .email(m.getEmail())
                .message(m.getMessage())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .build();
    }
}