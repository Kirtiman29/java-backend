package com.rdc.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when order cannot be cancelled.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OrderCancellationException extends RuntimeException {
    public OrderCancellationException(String message) {
        super(message);
    }
}