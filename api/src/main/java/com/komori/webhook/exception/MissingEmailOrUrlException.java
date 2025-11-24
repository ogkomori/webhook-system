package com.komori.webhook.exception;

import org.springframework.http.HttpStatus;

public class MissingEmailOrUrlException extends ApiException {
    public MissingEmailOrUrlException() {
        super("MISSING_EMAIL_OR_URL", "Please provide an email and a webhook URL", HttpStatus.BAD_REQUEST.value());
    }
}
