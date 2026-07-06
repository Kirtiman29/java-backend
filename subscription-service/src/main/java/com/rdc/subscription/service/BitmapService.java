package com.rdc.subscription.service;

import com.rdc.subscription.dto.bitmap.BitmapAnalyzeResponse;
import com.rdc.subscription.dto.bitmap.BitmapUploadResponse;
import com.rdc.subscription.dto.bitmap.GeminiImageToImageResponse;
import com.rdc.subscription.entity.BitmapMetadata;
import com.rdc.subscription.repository.BitmapMetadataRepository;
import com.rdc.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BitmapService {

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );

    private final WebClient bitmapWebClient;
    private final BitmapMetadataRepository bitmapMetadataRepository;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * Verifies that the user has an active subscription.
     */
    private void checkActiveSubscription(Long userId) {
        boolean hasActive = subscriptionRepository
                .findFirstByUserIdAndStatusAndEndDateAfterOrderByCreatedAtDesc(
                        userId,
                        com.rdc.subscription.enums.SubscriptionStatus.ACTIVE,
                        java.time.LocalDateTime.now()
                )
                .isPresent();
        if (!hasActive) {
            throw new RuntimeException("No active subscription found. Access to bitmap tools requires an active subscription.");
        }
    }

    /**
     * Validates that the file exists and is owned by the logged-in user.
     */
    private BitmapMetadata validateFileOwnership(String filename, Long userId) {
        if (filename == null || filename.isBlank()) {
            throw new RuntimeException("Filename parameter is required");
        }
        return bitmapMetadataRepository.findByStoredFilenameAndUserId(filename, userId)
                .orElseThrow(() -> new RuntimeException("File not found or access denied"));
    }

    private void validateImageFileType(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Cannot upload empty " + fieldName);
        }

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return;
        }

        String normalizedContentType = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(normalizedContentType)) {
            throw new RuntimeException("Unsupported image type for " + fieldName + ". Allowed types are PNG, JPEG, and WEBP.");
        }
    }

    private void addMultipartFile(MultipartBodyBuilder bodyBuilder, String partName, MultipartFile file) throws IOException {
        Resource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                String originalFilename = file.getOriginalFilename();
                return originalFilename != null && !originalFilename.isBlank() ? originalFilename : partName + ".png";
            }
        };

        var part = bodyBuilder.part(partName, resource);
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            try {
                part.contentType(MediaType.parseMediaType(contentType.split(";")[0].trim()));
            } catch (IllegalArgumentException ignored) {
                log.debug("Skipping invalid content type {} for multipart part {}", contentType, partName);
            }
        }
    }

    /**
     * Uploads the file to the FastAPI bitmap backend after validating the user's active subscription status.
     * Prevents overwriting by generating a unique file name.
     */
    public BitmapUploadResponse uploadImage(MultipartFile file, Long userId) {
        checkActiveSubscription(userId);

        if (file.isEmpty()) {
            throw new RuntimeException("Cannot upload empty file");
        }

        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = UUID.randomUUID().toString() + "_" + (originalFilename != null ? originalFilename : "image.png");

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        try {
            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return uniqueFilename;
                }
            };
            bodyBuilder.part("file", resource);
        } catch (IOException e) {
            log.error("Failed to read uploaded file content", e);
            throw new RuntimeException("Failed to read file contents");
        }

        try {
            BitmapUploadResponse response = bitmapWebClient.post()
                    .uri("/upload/")
                    .header("X-User-Id", String.valueOf(userId))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(BitmapUploadResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Received empty response from FastAPI upload endpoint");
            }

            // Store metadata in the database
            BitmapMetadata metadata = BitmapMetadata.builder()
                    .userId(userId)
                    .originalFilename(originalFilename != null ? originalFilename : "image.png")
                    .storedFilename(response.getFilename())
                    .bitmapId(response.getBitmapId())
                    .filePath(response.getPath())
                    .sizeBytes(response.getSizeBytes())
                    .build();

            bitmapMetadataRepository.save(metadata);

            return response;
        } catch (WebClientResponseException ex) {
            log.error("FastAPI upload failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Upload failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Failed to connect to FastAPI upload endpoint", ex);
            throw new RuntimeException("Bitmap service is currently unavailable");
        }
    }

    /**
     * Analyzes design type and style, updating stored metadata values.
     */
    public BitmapAnalyzeResponse analyzeImage(String filename, Long userId) {
        checkActiveSubscription(userId);
        BitmapMetadata metadata = validateFileOwnership(filename, userId);

        try {
            BitmapAnalyzeResponse response = bitmapWebClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/analyze/")
                            .queryParam("filename", filename)
                            .build())
                    .header("X-User-Id", String.valueOf(userId))
                    .retrieve()
                    .bodyToMono(BitmapAnalyzeResponse.class)
                    .block();

            if (response != null) {
                metadata.setDesignType(response.getDesignType());
                metadata.setConfidence(response.getConfidence());
                if (response.getSuggestedStyles() != null) {
                    metadata.setSuggestedStyles(String.join(",", response.getSuggestedStyles()));
                }
                if (response.getAnalysis() != null) {
                    metadata.setEdgeDensityScore(response.getAnalysis().getEdgeDensityScore());
                    metadata.setColorSaturationScore(response.getAnalysis().getColorSaturationScore());
                }
                bitmapMetadataRepository.save(metadata);
            }

            return response;
        } catch (WebClientResponseException ex) {
            log.error("FastAPI analysis failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Analysis failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Failed to connect to FastAPI analysis endpoint", ex);
            throw new RuntimeException("Analysis service is currently unavailable");
        }
    }

    /**
     * Forwards a multipart Gemini image-to-image recolor request to the FastAPI bitmap backend.
     * The response is returned to the caller without modification so the frontend can use the
     * relative output URLs exactly as produced by FastAPI.
     */
    public GeminiImageToImageResponse geminiImageToImage(
            MultipartFile file,
            MultipartFile maskFile,
            Map<String, String> params,
            Long userId
    ) {
        checkActiveSubscription(userId);
        validateImageFileType(file, "file");
        if (maskFile != null && !maskFile.isEmpty()) {
            validateImageFileType(maskFile, "mask_file");
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        Map<String, String> forwardedParams = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
        forwardedParams.remove("file");
        forwardedParams.remove("mask_file");

        try {
            addMultipartFile(bodyBuilder, "file", file);

            if (maskFile != null && !maskFile.isEmpty()) {
                addMultipartFile(bodyBuilder, "mask_file", maskFile);
            }

            if (!forwardedParams.isEmpty()) {
                forwardedParams.forEach((key, value) -> {
                    if (value != null && !value.isBlank()) {
                        bodyBuilder.part(key, value);
                    }
                });
            }

            GeminiImageToImageResponse response = bitmapWebClient.post()
                    .uri("/gemini-image/image-to-image")
                    .header("X-User-Id", String.valueOf(userId))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(GeminiImageToImageResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Received empty response from FastAPI image-to-image endpoint");
            }

            return response;
        } catch (WebClientResponseException ex) {
            log.error("FastAPI image-to-image failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Image-to-image failed: " + ex.getResponseBodyAsString());
        } catch (IOException ex) {
            log.error("Failed to read image content for FastAPI image-to-image request", ex);
            throw new RuntimeException("Failed to read image contents");
        } catch (Exception ex) {
            log.error("Failed to connect to FastAPI image-to-image endpoint", ex);
            throw new RuntimeException("Gemini image-to-image service is currently unavailable");
        }
    }

    /**
     * General purpose binary proxy mechanism. Checks subscription status, verifies ownership of the file,
     * forwards query parameters, and pipes binary streams from FastAPI back to Java.
     */
    public ResponseEntity<byte[]> forwardBinaryRequest(String path, Map<String, String> params, Long userId) {
        checkActiveSubscription(userId);

        String filename = params.get("filename");
        validateFileOwnership(filename, userId);

        try {
            ResponseEntity<byte[]> response = bitmapWebClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path(path);
                        params.forEach((key, value) -> {
                            if (value != null && !value.isBlank()) {
                                uriBuilder.queryParam(key, value);
                            }
                        });
                        return uriBuilder.build();
                    })
                    .header("X-User-Id", String.valueOf(userId))
                    .retrieve()
                    .toEntity(byte[].class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Received empty response from FastAPI service");
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(response.getHeaders().getContentType());
            headers.setContentDisposition(response.getHeaders().getContentDisposition());

            return new ResponseEntity<>(response.getBody(), headers, response.getStatusCode());

        } catch (WebClientResponseException ex) {
            log.error("FastAPI error on path {}: status={}, body={}", path, ex.getStatusCode(), ex.getResponseBodyAsString());
            // Forward clean error from FastAPI to the frontend
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(ex.getResponseBodyAsByteArray(), headers, ex.getStatusCode());
        } catch (Exception ex) {
            log.error("Failed to forward binary request to FastAPI path {}", path, ex);
            throw new RuntimeException("Bitmap processing failed: " + ex.getMessage());
        }
    }
}
