package com.rdc.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when Cart Service is unavailable.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class CartServiceException extends RuntimeException {
    public CartServiceException(String message) {
        super(message);
    }

    public CartServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}