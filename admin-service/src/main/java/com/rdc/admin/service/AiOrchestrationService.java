package com.rdc.admin.service;

import com.rdc.admin.client.FastApiClient;
import com.rdc.admin.client.SubscriptionServiceClient;
import com.rdc.admin.dto.ai.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiOrchestrationService {

    private final AiToolCostService aiToolCostService;
    private final SubscriptionServiceClient subscriptionServiceClient;
    private final FastApiClient fastApiClient;

    public AiToolResponse executeTool(Long userId, AiToolRequest request) {
        int cost = aiToolCostService.getCost(request.getToolName());

        SubscriptionAiValidationResponse validation =
                subscriptionServiceClient.validateAi(userId, request.getToolName(), cost);

        if (validation == null || !validation.isAllowed()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    validation != null ? validation.getMessage() : "AI validation failed"
            );
        }

        FastApiExecuteRequest fastApiRequest = FastApiExecuteRequest.builder()
                .requestId(UUID.randomUUID().toString())
                .userId(userId)
                .toolName(request.getToolName())
                .inputUrl(request.getInputUrl())
                .params(request.getParams())
                .build();

        FastApiExecuteResponse fastApiResponse = fastApiClient.execute(fastApiRequest);

        if (fastApiResponse == null || !Boolean.TRUE.equals(fastApiResponse.getSuccess())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    fastApiResponse != null ? fastApiResponse.getMessage() : "FastAPI execution failed"
            );
        }

        SubscriptionAiValidationResponse consume =
                subscriptionServiceClient.consumeAi(userId, request.getToolName(), cost);

        return AiToolResponse.builder()
                .success(true)
                .toolName(request.getToolName())
                .message(fastApiResponse.getMessage())
                .outputUrl(fastApiResponse.getOutputUrl())
                .outputData(fastApiResponse.getOutputData())
                .remainingCredits(consume != null ? consume.getAvailableCredits() : null)
                .build();
    }
}
