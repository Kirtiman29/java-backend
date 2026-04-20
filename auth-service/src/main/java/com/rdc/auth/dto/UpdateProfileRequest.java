package com.rdc.auth.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String displayName;
    private String email;
    private String oldPassword;
    private String newPassword;
}