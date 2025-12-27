package com.rdc.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a design exists but is not available for purchase.
 * (e.g., draft status, inactive, no price set)
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DesignNotAvailableException extends RuntimeException {
    public DesignNotAvailableException(String message) {
        super(message);
    }
}