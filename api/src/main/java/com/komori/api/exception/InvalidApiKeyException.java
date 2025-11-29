package com.komori.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidApiKeyException extends ApiException {
    public InvalidApiKeyException() {
        super("UNAUTHORIZED", "Unauthorized", HttpStatus.UNAUTHORIZED.value());
    }
}
