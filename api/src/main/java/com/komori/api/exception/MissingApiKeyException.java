package com.komori.api.exception;

import org.springframework.http.HttpStatus;

public class MissingApiKeyException extends ApiException {
    public MissingApiKeyException() {
        super("MISSING_API_KEY", "Missing API key", HttpStatus.BAD_REQUEST.value());
    }
}
