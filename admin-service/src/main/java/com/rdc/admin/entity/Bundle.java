package com.rdc.admin.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Entity
@Table(name = "bundle")
@Data
public class Bundle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Lob
    private String description;

    // Maps to the bundle_designs join table
    @ManyToMany
    @JoinTable(
            name = "bundle_designs",
            joinColumns = @JoinColumn(name = "bundle_id"),
            inverseJoinColumns = @JoinColumn(name = "design_id")
    )
    private List<Design> designs; // List<Design> not List<Long> as per JPA best practice

    private Integer priceCents;
    private boolean active = true;
    private LocalDateTime createdAt = LocalDateTime.now();
}