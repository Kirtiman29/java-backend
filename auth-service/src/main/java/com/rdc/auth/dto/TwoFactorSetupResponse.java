package com.rdc.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorSetupResponse {
    private String secret;
    private String qrCodeImageUri;
    private String provisioningUri;
}
