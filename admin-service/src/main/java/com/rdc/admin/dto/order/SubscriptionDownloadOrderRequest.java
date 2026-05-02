package com.rdc.admin.dto.order;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionDownloadOrderRequest {
    private Long userId;
    private Long designId;
    private String designIdentifier;
    private String designTitle;
    private String assetUuid;
    private Integer remainingDesigns;
}
