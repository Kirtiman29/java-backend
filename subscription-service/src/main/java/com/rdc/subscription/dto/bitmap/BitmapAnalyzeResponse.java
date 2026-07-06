package com.rdc.subscription.dto.bitmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BitmapAnalyzeResponse {
    private String status;
    private String filename;
    private String designType;
    private Double confidence;
    private List<String> suggestedStyles;
    private AnalysisDetails analysis;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisDetails {
        private Double edgeDensityScore;
        private Double colorSaturationScore;
    }
}
