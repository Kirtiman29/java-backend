// File: com.rdc.admin.entity.Category.java
package com.rdc.admin.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    private boolean active = true;

    private Integer sortOrder = 0;

    private LocalDateTime createdAt;

    // Many-to-One or One-to-Many relationships to Designs/Bundles would go here
}