package com.rdc.admin.controller;

import com.rdc.admin.service.DesignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/internal/designs")
@RequiredArgsConstructor
@Slf4j
public class InternalDesignController {

    private final DesignService designService;

    @Value("${internal.service.key}")
    private String internalServiceKey;

    /**
     * Internal endpoint called by Order Service after successful payment.
     * Triggers the logic to mark a design as sold (inactive + draft).
     */
    @PostMapping("/{designId}/sold")
    public ResponseEntity<Void> markDesignSold(
            @PathVariable Long designId,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {

        // Validate the service-to-service shared secret
        validateKey(key);

        log.info("🔒 Authorized internal request: Marking design {} as SOLD", designId);

        // Sets active=false and draft=true to remove from storefront
        designService.markDesignAsSold(designId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{designId}/purge")
    public ResponseEntity<Void> purgeDesign(
            @PathVariable Long designId,
            @RequestParam(required = false) Long orderId,
            @RequestHeader(value = "X-INTERNAL-KEY", required = false) String key) {

        validateKey(key);

        log.info("🔥 Authorized internal request: Purging design {} for Order {}", designId, orderId);
        designService.purgeDesignAndRecord(designId, orderId);

        return ResponseEntity.ok().build();
    }

    private void validateKey(String key) {
        if (internalServiceKey == null || !internalServiceKey.equals(key)) {
            log.error("❌ Access Denied: Invalid or missing X-INTERNAL-KEY header");
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Invalid internal service key");
        }
    }
}