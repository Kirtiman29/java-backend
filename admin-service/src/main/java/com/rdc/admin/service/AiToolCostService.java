package com.rdc.admin.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Service
public class AiToolCostService {

    private static final Map<String, Integer> TOOL_COSTS = Map.of(
            "UPSCALE", 5,
            "TEXTILE_GENERATOR", 10,
            "GEMINI_TEXT_TO_IMAGE", 10,
            "PATTERN_GENERATOR", 7,
            "COLOR_SEPARATION", 4,
            "COLORWAY", 4,
            "IMAGE_MIX", 8,
            "PROMPT_ENHANCER", 2
    );

    public int getCost(String toolName) {
        Integer cost = TOOL_COSTS.get(toolName);
        if (cost == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported AI tool: " + toolName);
        }
        return cost;
    }
}
