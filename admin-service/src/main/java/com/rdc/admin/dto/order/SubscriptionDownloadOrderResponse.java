package com.rdc.admin.dto.order;

import lombok.Data;

@Data
public class SubscriptionDownloadOrderResponse {
    private Long orderId;
    private String status;
    private String purchaseType;
}
