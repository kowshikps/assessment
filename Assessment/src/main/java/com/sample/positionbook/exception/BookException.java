package com.sample.positionbook.exception;

import org.springframework.http.HttpStatus;

public class BookException extends RuntimeException {
    public final HttpStatus status;
    public BookException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
