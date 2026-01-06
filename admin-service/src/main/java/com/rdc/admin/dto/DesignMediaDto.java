package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class DesignMediaDto {
    private String url;
    private String type;
    private Boolean primary;
}