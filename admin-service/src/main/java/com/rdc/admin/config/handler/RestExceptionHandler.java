package com.rdc.admin.config.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException; // <-- Important Import

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class RestExceptionHandler {

    /**
     * Handles exceptions that extend ResponseStatusException (e.g., ResourceNotFoundException)
     * This handler manually builds the body to ensure the "trace" field is omitted.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", ex.getReason()); // Use the reason set in ResourceNotFoundException constructor
        body.put("path", request.getDescription(false).substring(4));

        return new ResponseEntity<>(body, status);
    }

    /**
     * Keeps the handler for @Valid DTO validation failures (HTTP 400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Validation failed for request body.");
        body.put("details", errors);
        body.put("path", request.getDescription(false).substring(4));

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}