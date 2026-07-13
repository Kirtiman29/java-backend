package com.rdc.subscription.dto.bitmap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeminiImageToImageResponse {
    private Boolean success;
    private String message;

    @JsonProperty("input_prompt")
    private String inputPrompt;

    @JsonProperty("final_prompt")
    private String finalPrompt;

    @JsonProperty("source_color")
    private String sourceColor;

    @JsonProperty("target_color")
    private String targetColor;

    @JsonProperty("edit_type")
    private String editType;

    private String model;

    @JsonProperty("aspect_ratio")
    private String aspectRatio;

    @JsonProperty("num_images")
    private Integer numImages;

    @JsonProperty("edit_mode")
    private String editMode;

    @JsonProperty("edit_options")
    private Map<String, Object> editOptions;

    @JsonProperty("mask_used")
    private Boolean maskUsed;

    private List<String> filenames;

    @JsonProperty("image_urls")
    private List<String> imageUrls;

    private String filename;

    @JsonProperty("output_url")
    private String outputUrl;

    @JsonProperty("remaining_credits")
    private Integer remainingCredits;

    @JsonProperty("credits_required")
    private Integer creditsRequired;
}
