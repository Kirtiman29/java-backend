package com.rdc.admin.controller;

import com.rdc.admin.dto.ai.AiToolRequest;
import com.rdc.admin.dto.ai.AiToolResponse;
import com.rdc.admin.service.AiOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
public class AiController {

    private final AiOrchestrationService aiOrchestrationService;

    @PostMapping("/use")
    public ResponseEntity<AiToolResponse> useAiTool(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AiToolRequest request
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        return ResponseEntity.ok(aiOrchestrationService.executeTool(userId, request));
    }
}
