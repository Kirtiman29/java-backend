package com.rdc.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String displayName;
    private String role;
    private boolean isVerified;
    private boolean isTwoFactorEnabled; // To be added to User entity
    
    // External data from other services
    private SubscriptionDetails subscription;
    private CreditDetails credits;

    @Data
    @Builder
    public static class SubscriptionDetails {
        private String planName;
        private String status;
        private Long expiresAt;
    }

    @Data
    @Builder
    public static class CreditDetails {
        private Integer totalCredits;
        private Integer usedCredits;
    }
}