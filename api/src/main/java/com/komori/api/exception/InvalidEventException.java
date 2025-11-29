package com.komori.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidEventException extends ApiException {
    public InvalidEventException() {
        super("INVALID_EVENT", "Payload must be a valid JSON object", HttpStatus.BAD_REQUEST.value());
    }
}
