package com.komori.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidEmailException extends ApiException {
    public InvalidEmailException() {
        super("INVALID_EMAIL", "Please enter a valid email", HttpStatus.BAD_REQUEST.value());
    }
}
