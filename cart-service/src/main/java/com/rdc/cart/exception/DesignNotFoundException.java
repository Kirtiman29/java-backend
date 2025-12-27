package com.rdc.cart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a design is not found in Admin Service.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DesignNotFoundException extends RuntimeException {
    public DesignNotFoundException(String message) {
        super(message);
    }
}