package com.rdc.admin.controller;

import com.rdc.admin.dto.TransactionIngestRequest;
import com.rdc.admin.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
// Changed mapping to handle transactions separately from the main admin API
// Transaction ingestion is a webhook, not a user-facing admin action
@RequestMapping("/api/webhooks/transactions")
public class AdminTransactionController {

    private final TransactionService transactionService;

    // This endpoint is now /api/webhooks/transactions/ingest
    @PostMapping("/ingest")
    public ResponseEntity<Void> ingestTransaction(@Valid @RequestBody TransactionIngestRequest request) {
        // NOTE: In a production app, you MUST implement API Key or HMAC
        // authentication here, as this endpoint is now public to the webhook caller.

        transactionService.ingestTransaction(request);
        return ResponseEntity.ok().build();
    }
}