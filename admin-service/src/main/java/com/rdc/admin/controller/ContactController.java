package com.rdc.admin.controller;

import com.rdc.admin.dto.ContactRequest;
import com.rdc.admin.dto.ContactResponse;
import com.rdc.admin.entity.ContactMessage;
import com.rdc.admin.service.ContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ContactController {

    private final ContactService contactService;

    /**
     * 🌍 PUBLIC – Accessible without a token.
     * Use this for the storefront contact form.
     */
    @PostMapping("/public/contact")
    public ResponseEntity<Map<String, String>> submitContact(@Valid @RequestBody ContactRequest request) {
        contactService.submit(request);

        return ResponseEntity.ok(
                Map.of("message", "Contact submitted successfully")
        );
    }

    /**
     * 🔐 ADMIN – List all messages.
     * Used for the main admin dashboard table.
     */
    @GetMapping("/admin/contacts")
    public ResponseEntity<List<ContactResponse>> getAll() {
        return ResponseEntity.ok(contactService.getAll());
    }

    /**
     * 🔐 ADMIN – Fetch single message details (Check Status).
     * Used when clicking a specific message to view details.
     */
    @GetMapping("/admin/contacts/{id}")
    public ResponseEntity<ContactResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(contactService.getById(id));
    }

    /**
     * 🔐 ADMIN – Update status using JSON body.
     * Body: { "status": "READ" } or { "status": "REPLIED" }
     */
    @PatchMapping("/admin/contacts/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String statusStr = body.get("status");
        if (statusStr == null) {
            return ResponseEntity.badRequest().build();
        }

        ContactMessage.Status status = ContactMessage.Status.valueOf(statusStr.toUpperCase());
        contactService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }
}