package com.rdc.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    private String location;
    private String experienceLevel;
    private String jobType;
}