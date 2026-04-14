package com.rdc.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponse {
    private int successCount;
    private int failureCount;
    private List<String> errors;
}