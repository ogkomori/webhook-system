package com.komori.webhook.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApiException {
    public EmailAlreadyExistsException() {
        super("Email already exists", "EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT.value());
    }
}
