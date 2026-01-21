// src/main/java/com/rdc/admin/dto/DesignMediaDto.java
package com.rdc.admin.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesignMediaDto {
    private String url;
    private String type; // Will hold IMAGE, VIDEO, etc.
    private String role; // Will hold COVER, GALLERY, etc.
}