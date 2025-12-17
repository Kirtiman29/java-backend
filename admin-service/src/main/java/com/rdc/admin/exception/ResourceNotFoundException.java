package com.rdc.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    // Fix: Add this constructor that only takes a message
    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Keep your existing one if you use it elsewhere
    public ResourceNotFoundException(String resourceName, Long id) {
        super(String.format("%s not found with id: %d", resourceName, id));
    }
}