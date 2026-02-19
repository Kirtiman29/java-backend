package com.rdc.admin.service;

import com.rdc.admin.dto.TransactionIngestRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {


    public void ingestTransaction(TransactionIngestRequest request) {

        System.out.println("Processing incoming transaction webhook for ID: " + request.getTransactionId());
    }
}