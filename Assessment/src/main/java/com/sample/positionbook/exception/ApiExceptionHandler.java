package com.sample.positionbook.exception;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    public static class ApiError {
        public final Instant timestamp = Instant.now();
        public final int status;
        public final String message;
        public final String path;
        public final List<String> details;
        public ApiError(int status, String message, String path, List<String> details) {
            this.status = status; this.message = message; this.path = path; this.details = details;
        }
    }

    @ExceptionHandler(BookException.class)
    public ResponseEntity<ApiError> book(BookException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.status).body(new ApiError(ex.status.value(), ex.getMessage(), request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(400, "Input validation failed", request.getRequestURI(),
                ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).sorted().collect(Collectors.toList())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformed(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(400,
                "Invalid JSON: use BUY, SELL or CANCEL and integer numbers; unknown fields are rejected", request.getRequestURI(), List.of()));
    }
}
