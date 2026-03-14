package com.rdc.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {

    @NotNull
    private Long userId;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer email is required")
    private String customerEmail;

    private String customerPhone;

    @NotBlank(message = "Billing state is required")
    private String billingState;

    private String customerGstin;

    private String organizationName;

    @NotBlank(message = "Address line 1 is required")
    private String addressOne;

    private String addressTwo;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "Pincode is required")
    private String pincode;

    private List<OrderItemRequest> items;
}