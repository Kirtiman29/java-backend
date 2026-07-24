package com.rdc.subscription.service;

import com.rdc.subscription.dto.bitmap.BitmapAnalyzeResponse;
import com.rdc.subscription.dto.bitmap.BitmapJobResponse;
import com.rdc.subscription.dto.bitmap.BitmapUploadResponse;
import com.rdc.subscription.dto.bitmap.GeminiImageToImageResponse;
import com.rdc.subscription.dto.internal.AiValidationRequest;
import com.rdc.subscription.dto.internal.AiValidationResponse;
import com.rdc.subscription.dto.internal.ConsumeAiRequest;
import com.rdc.subscription.entity.BitmapMetadata;
import com.rdc.subscription.entity.BitmapQueuedJob;
import com.rdc.subscription.repository.BitmapMetadataRepository;
import com.rdc.subscription.repository.BitmapQueuedJobRepository;
import com.rdc.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BitmapService {

    private static final String GEMINI_IMAGE_TO_IMAGE_TOOL = "GEMINI_IMAGE_TO_IMAGE";
    private static final int GEMINI_IMAGE_TO_IMAGE_CREDIT_COST = 8;
    private static final String EMBROIDERY_PREVIEW_TOOL = "EMBROIDERY_PREVIEW";
    private static final int EMBROIDERY_PREVIEW_CREDIT_COST = 10;
    private static final String BITMAP_TOOL = "BITMAP";
    private static final int BITMAP_CREDIT_COST = 10;

    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp"
    );

    @Qualifier("bitmapWebClient")
    private final WebClient bitmapWebClient;

    @Qualifier("geminiWebClient")
    private final WebClient geminiWebClient;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    private final BitmapMetadataRepository bitmapMetadataRepository;
    private final BitmapQueuedJobRepository bitmapQueuedJobRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EntitlementService entitlementService;

    /**
     * Verifies that the user has an active subscription.
     */
    private void validateGeminiImageToImageCredits(Long userId, int creditsRequired) {
        AiValidationRequest request = new AiValidationRequest();
        request.setUserId(userId);
        request.setToolName(GEMINI_IMAGE_TO_IMAGE_TOOL);
        request.setCreditsRequired(creditsRequired);

        AiValidationResponse validation = entitlementService.validateAiUsage(request);
        if (!validation.isAllowed()) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    validation.getMessage() != null ? validation.getMessage() : "AI credits are not available"
            );
        }
    }

    private AiValidationResponse consumeGeminiImageToImageCredits(Long userId, int creditsRequired) {
        ConsumeAiRequest request = new ConsumeAiRequest();
        request.setUserId(userId);
        request.setToolName(GEMINI_IMAGE_TO_IMAGE_TOOL);
        request.setCreditsRequired(creditsRequired);
        return entitlementService.consumeAiUsage(request);
    }
    private void validateAiCredits(Long userId, String toolName, int creditsRequired) {
        AiValidationRequest request = new AiValidationRequest();
        request.setUserId(userId);
        request.setToolName(toolName);
        request.setCreditsRequired(creditsRequired);

        AiValidationResponse validation = entitlementService.validateAiUsage(request);
        if (!validation.isAllowed()) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    validation.getMessage() != null ? validation.getMessage() : "AI credits are not available"
            );
        }
    }

    private AiValidationResponse consumeAiCredits(Long userId, String toolName, int creditsRequired) {
        ConsumeAiRequest request = new ConsumeAiRequest();
        request.setUserId(userId);
        request.setToolName(toolName);
        request.setCreditsRequired(creditsRequired);
        return entitlementService.consumeAiUsage(request);
    }

    private int resolveGeminiImageToImageCredits(Map<String, String> params) {
        int outputCount = 1;
        if (params != null) {
            String rawCount = params.getOrDefault("num_images", params.get("numImages"));
            if (rawCount != null && !rawCount.isBlank()) {
                try {
                    outputCount = Math.max(Integer.parseInt(rawCount.trim()), 1);
                } catch (NumberFormatException ignored) {
                    outputCount = 1;
                }
            }
        }
        return GEMINI_IMAGE_TO_IMAGE_CREDIT_COST * outputCount;
    }
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
        int creditsRequired = resolveGeminiImageToImageCredits(params);
        validateGeminiImageToImageCredits(userId, creditsRequired);
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
            bodyBuilder.part("userId", String.valueOf(userId));

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

            GeminiImageToImageResponse response = geminiWebClient.post()
                    .uri("/gemini-image/image-to-image")
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-INTERNAL-KEY", internalServiceKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(GeminiImageToImageResponse.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Received empty response from FastAPI image-to-image endpoint");
            }

            AiValidationResponse creditResponse = consumeGeminiImageToImageCredits(userId, creditsRequired);
            response.setCreditsRequired(creditResponse.getCreditsRequired());
            response.setRemainingCredits(creditResponse.getAvailableCredits());

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

    public ResponseEntity<Map> embroideryPreview(
            MultipartFile image,
            Map<String, String> params,
            Long userId
    ) {
        checkActiveSubscription(userId);
        validateAiCredits(userId, EMBROIDERY_PREVIEW_TOOL, EMBROIDERY_PREVIEW_CREDIT_COST);
        validateImageFileType(image, "image");

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        Map<String, String> forwardedParams = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
        forwardedParams.remove("image");
        forwardedParams.remove("file");

        try {
            addMultipartFile(bodyBuilder, "image", image);
            bodyBuilder.part("user_id", String.valueOf(userId));

            if (!forwardedParams.isEmpty()) {
                forwardedParams.forEach((key, value) -> {
                    if (value != null && !value.isBlank()) {
                        bodyBuilder.part(key, value);
                    }
                });
            }

            Map response = geminiWebClient.post()
                    .uri("/api/embroidery-preview")
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-INTERNAL-KEY", internalServiceKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("Received empty response from FastAPI embroidery endpoint");
            }

            AiValidationResponse creditResponse = consumeAiCredits(userId, EMBROIDERY_PREVIEW_TOOL, EMBROIDERY_PREVIEW_CREDIT_COST);
            response.put("remaining_credits", creditResponse.getAvailableCredits());
            response.put("credits_required", creditResponse.getCreditsRequired());

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Remaining-Credits", String.valueOf(creditResponse.getAvailableCredits()));
            headers.add("X-Credits-Required", String.valueOf(creditResponse.getCreditsRequired()));

            return new ResponseEntity<>(response, headers, HttpStatus.OK);
        } catch (WebClientResponseException ex) {
            log.error("FastAPI embroidery failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Embroidery preview failed: " + ex.getResponseBodyAsString());
        } catch (IOException ex) {
            log.error("Failed to read image content for FastAPI embroidery request", ex);
            throw new RuntimeException("Failed to read image contents");
        } catch (Exception ex) {
            log.error("Failed to connect to FastAPI embroidery endpoint", ex);
            throw new RuntimeException("Embroidery preview service is currently unavailable");
        }
    }

    public BitmapJobResponse queueBitmapRequest(String path, Map<String, String> params, Long userId) {
        checkActiveSubscription(userId);
        validateAiCredits(userId, BITMAP_TOOL, BITMAP_CREDIT_COST);

        Map<String, String> forwardedParams = params == null ? new LinkedHashMap<>() : new LinkedHashMap<>(params);
        String filename = forwardedParams.get("filename");
        validateFileOwnership(filename, userId);

        String featureName = resolveBitmapFeatureName(path);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("featureName", featureName);
        body.put("params", forwardedParams);

        try {
            Map response = bitmapWebClient.post()
                    .uri("/internal/ai/jobs")
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-INTERNAL-KEY", internalServiceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            BitmapJobResponse job = parseBitmapJobResponse(response, featureName);
            if (job == null || !Boolean.TRUE.equals(job.getSuccess()) || job.getJobId() == null) {
                throw new RuntimeException(job != null && job.getMessage() != null ? job.getMessage() : "Bitmap job queue failed");
            }

            bitmapQueuedJobRepository.findByJobId(job.getJobId()).orElseGet(() ->
                    bitmapQueuedJobRepository.save(
                            BitmapQueuedJob.builder()
                                    .jobId(job.getJobId())
                                    .userId(userId)
                                    .featureName(featureName)
                                    .creditsRequired(BITMAP_CREDIT_COST)
                                    .consumed(false)
                                    .status(job.getStatus())
                                    .outputKey(job.getOutputKey())
                                    .errorMessage(job.getErrorMessage())
                                    .build()
                    )
            );

            job.setCreditsRequired(BITMAP_CREDIT_COST);
            job.setCreditsConsumed(false);
            job.setRemainingCredits(null);
            job.setQueued(isActiveQueuedStatus(job.getStatus()));
            return job;
        } catch (WebClientResponseException ex) {
            log.error("FastAPI bitmap job queue failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Bitmap job queue failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Failed to queue bitmap job for path {}", path, ex);
            throw new RuntimeException("Bitmap job queue failed: " + ex.getMessage());
        }
    }

    @Transactional
    public BitmapJobResponse getBitmapJobStatus(Long jobId, Long userId) {
        if (jobId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "jobId is required");
        }

        BitmapJobResponse job = fetchBitmapJobStatus(jobId);
        if (job == null || !Boolean.TRUE.equals(job.getSuccess())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    job != null ? firstNonBlank(job.getMessage(), job.getErrorMessage()) : "Unable to fetch bitmap job status"
            );
        }

        if (job.getUserId() != null && !Objects.equals(job.getUserId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bitmap job access denied");
        }

        BitmapQueuedJob queuedJob = bitmapQueuedJobRepository.findByJobIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bitmap job not found"));

        queuedJob.setStatus(job.getStatus());
        queuedJob.setOutputKey(job.getOutputKey());
        queuedJob.setErrorMessage(job.getErrorMessage());

        Integer remainingCredits = null;
        if ("COMPLETED".equalsIgnoreCase(job.getStatus()) && !queuedJob.isConsumed()) {
            AiValidationResponse creditResponse = consumeAiCredits(
                    queuedJob.getUserId(),
                    BITMAP_TOOL,
                    queuedJob.getCreditsRequired()
            );
            queuedJob.markConsumed();
            remainingCredits = creditResponse.getAvailableCredits();
        }

        bitmapQueuedJobRepository.save(queuedJob);

        job.setCreditsRequired(queuedJob.getCreditsRequired());
        job.setCreditsConsumed(queuedJob.isConsumed());
        job.setRemainingCredits(remainingCredits);
        job.setQueued(isActiveQueuedStatus(job.getStatus()));
        return job;
    }

    private BitmapJobResponse fetchBitmapJobStatus(Long jobId) {
        try {
            Map response = bitmapWebClient.get()
                    .uri("/internal/ai/jobs/{jobId}", jobId)
                    .header("X-INTERNAL-KEY", internalServiceKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return parseBitmapJobResponse(response, null);
        } catch (WebClientResponseException ex) {
            log.error("FastAPI bitmap job status failed: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new RuntimeException("Bitmap job status failed: " + ex.getResponseBodyAsString());
        } catch (Exception ex) {
            log.error("Failed to fetch bitmap job status for jobId={}", jobId, ex);
            throw new RuntimeException("Bitmap job status failed: " + ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private BitmapJobResponse parseBitmapJobResponse(Map payload, String fallbackFeatureName) {
        if (payload == null || payload.isEmpty()) {
            return null;
        }

        Map<String, Object> record = (Map<String, Object>) payload;
        Map<String, Object> input = null;
        Object rawInput = record.get("input");
        if (rawInput instanceof Map<?, ?> inputMap) {
            Map<String, Object> parsedInput = new LinkedHashMap<>();
            inputMap.forEach((key, value) -> parsedInput.put(String.valueOf(key), value));
            input = parsedInput;
        }

        String status = stringValue(record, "status");
        return BitmapJobResponse.builder()
                .success(booleanValue(record.get("success"), status))
                .jobId(longValue(record.get("jobId"), record.get("job_id"), record.get("id")))
                .userId(longValue(record.get("userId"), record.get("user_id")))
                .featureName(firstNonBlank(stringValue(record, "featureName", "feature_name", "toolName", "tool_name"), fallbackFeatureName))
                .workerType(stringValue(record, "workerType", "worker_type"))
                .status(status)
                .outputKey(stringValue(record, "outputKey", "output_key", "outputUrl", "output_url"))
                .errorMessage(stringValue(record, "errorMessage", "error_message", "error"))
                .message(stringValue(record, "message"))
                .input(input)
                .queued(isActiveQueuedStatus(status))
                .build();
    }

    private String resolveBitmapFeatureName(String path) {
        String normalized = path == null ? "" : path.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("/halftone/separation/proof")) {
            return "BITMAP_SEPARATION_PROOF";
        }
        if (normalized.contains("/halftone/separation/psd")) {
            return "BITMAP_SEPARATION_PSD";
        }
        if (normalized.contains("/halftone/separation")) {
            return "BITMAP_SEPARATION";
        }
        if (normalized.contains("/halftone/cmyk")) {
            return "BITMAP_CMYK";
        }
        if (normalized.contains("/dither")) {
            return "BITMAP_DITHER";
        }
        if (normalized.contains("/halftone/monochrome")) {
            return "BITMAP_HALFTONE";
        }
        return "BITMAP";
    }

    private boolean isActiveQueuedStatus(String status) {
        if (status == null || status.isBlank()) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return "CREATED".equals(normalized)
                || "QUEUED".equals(normalized)
                || "PROCESSING".equals(normalized);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String stringValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private Long longValue(Object... values) {
        for (Object value : values) {
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && !text.isBlank()) {
                try {
                    return Long.parseLong(text.trim());
                } catch (NumberFormatException ignored) {
                    // Try the next candidate.
                }
            }
        }
        return null;
    }

    private Boolean booleanValue(Object... values) {
        for (Object value : values) {
            if (value instanceof Boolean bool) {
                return bool;
            }
            if (value instanceof String text) {
                String normalized = text.trim();
                if ("true".equalsIgnoreCase(normalized)
                        || "success".equalsIgnoreCase(normalized)
                        || "created".equalsIgnoreCase(normalized)
                        || "queued".equalsIgnoreCase(normalized)
                        || "processing".equalsIgnoreCase(normalized)
                        || "completed".equalsIgnoreCase(normalized)) {
                    return true;
                }
                if ("false".equalsIgnoreCase(normalized)
                        || "failed".equalsIgnoreCase(normalized)
                        || "error".equalsIgnoreCase(normalized)) {
                    return false;
                }
            }
        }
        return null;
    }


    /**
     * General purpose binary proxy mechanism. Checks subscription status, verifies ownership of the file,
     * forwards query parameters, and pipes binary streams from FastAPI back to Java.
     */
    public ResponseEntity<byte[]> forwardBinaryRequest(String path, Map<String, String> params, Long userId) {
        checkActiveSubscription(userId);
        validateAiCredits(userId, BITMAP_TOOL, BITMAP_CREDIT_COST);

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
            AiValidationResponse creditResponse = consumeAiCredits(userId, BITMAP_TOOL, BITMAP_CREDIT_COST);
            headers.add("X-Remaining-Credits", String.valueOf(creditResponse.getAvailableCredits()));
            headers.add("X-Credits-Required", String.valueOf(creditResponse.getCreditsRequired()));

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


