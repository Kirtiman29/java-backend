package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class FabricResponse {

    private Long id;
    private String fabricIdentifier;
    private String slug;
    private String title;
    private String description;

    private Double pricePerMeter;
    private Double stockMeters;

    private String material;
    private Double width;
    private Integer gsm;
    private String length;

    private Long categoryId;
    private String assetUuid;
    @Builder.Default
    private List<FabricMediaDto> media = new ArrayList<>();
    private Boolean active;
}
