package com.rdc.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// This custom exception automatically maps to HTTP 404
public class ResourceNotFoundException extends ResponseStatusException {

    public ResourceNotFoundException(String resourceName, Long id) {
        // Use HttpStatus.NOT_FOUND (404) and provide a descriptive reason
        super(HttpStatus.NOT_FOUND, String.format("%s not found with id %d", resourceName, id));
    }
}