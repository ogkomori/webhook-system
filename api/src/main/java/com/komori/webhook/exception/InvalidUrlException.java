package com.komori.webhook.exception;

import org.springframework.http.HttpStatus;

public class InvalidUrlException extends ApiException {
    public InvalidUrlException() {
        super("INVALID_URL", "Please enter a valid url", HttpStatus.BAD_REQUEST.value());
    }
}
