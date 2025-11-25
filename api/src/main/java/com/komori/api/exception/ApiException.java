package com.komori.api.exception;

import lombok.Getter;

@Getter
public abstract class ApiException extends RuntimeException {
    private final String errorKey;
    private final int status;

    public ApiException(String errorKey, String message, int status) {
        super(message);
        this.errorKey = errorKey;
        this.status = status;
    }
}
