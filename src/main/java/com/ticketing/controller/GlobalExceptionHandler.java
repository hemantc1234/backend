package com.ticketing.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> notFound(NoSuchElementException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> badCredentials(BadCredentialsException ex) {
        return body(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> forbidden(AccessDeniedException ex) {
        return body(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> general(Exception ex) {
        log.error("Unhandled: {}", ex.getMessage(), ex);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status", status.value(),
                "error",  status.getReasonPhrase(),
                "message", message,
                "timestamp", Instant.now().toString()));
    }
}
