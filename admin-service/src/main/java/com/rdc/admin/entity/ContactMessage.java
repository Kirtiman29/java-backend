package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Builder.Default // ✅ FIX: Ensures Builder uses the default value
    private Status status = Status.NEW;

    @Builder.Default // ✅ FIX: Ensures Builder uses the current time
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status { NEW, READ, REPLIED }
}