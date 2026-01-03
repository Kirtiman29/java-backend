package com.rdc.wishlist.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DesignNotAvailableException extends RuntimeException {
    public DesignNotAvailableException(String message) {
        super(message);
    }
}