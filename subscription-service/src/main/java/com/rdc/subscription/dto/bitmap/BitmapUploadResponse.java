package com.rdc.subscription.dto.bitmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitmapUploadResponse {
    private String message;
    private String filename;
    private String path;
    private Long sizeBytes;
    private Long bitmapId;
}
