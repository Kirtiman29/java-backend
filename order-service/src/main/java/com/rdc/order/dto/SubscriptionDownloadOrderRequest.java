package com.rdc.order.dto;

import lombok.Data;

@Data
public class SubscriptionDownloadOrderRequest {
    private Long userId;
    private Long designId;
    private String designIdentifier;
    private String designTitle;
    private String assetUuid;
    private Integer remainingDesigns;
}
