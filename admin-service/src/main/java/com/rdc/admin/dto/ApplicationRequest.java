package com.rdc.admin.dto;

import jakarta.validation.constraints.*;
        import lombok.Data;

@Data
public class ApplicationRequest {
    @NotNull
    private Long jobId;
    @NotBlank
    private String fullName;
    @Email
    private String email;
    private String phone;
    @NotBlank
    private String resumeAssetUuid;
    private String portfolioAssetUuid;
}
