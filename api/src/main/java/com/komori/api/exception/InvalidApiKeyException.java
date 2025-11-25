package com.komori.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidApiKeyException extends ApiException {
    public InvalidApiKeyException() {
        super("INVALID_API_KEY", "Please enter a valid API key", HttpStatus.UNAUTHORIZED.value());
    }
}
