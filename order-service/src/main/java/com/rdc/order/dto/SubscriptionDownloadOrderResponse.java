package com.rdc.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionDownloadOrderResponse {
    private Long orderId;
    private String status;
    private String purchaseType;
}
