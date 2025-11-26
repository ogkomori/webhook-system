package com.komori.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidEventException extends ApiException {
    public InvalidEventException() {
        super("INVALID_EVENT", "Please enter a valid event format in the request body", HttpStatus.BAD_REQUEST.value());
    }
}
