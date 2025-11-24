package com.komori.webhook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Component
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ExceptionDescription> handleCustomException(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(
                ExceptionDescription.builder()
                        .error(e.getErrorKey())
                        .message(e.getMessage())
                        .code(e.getStatus())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDescription> handleGeneralException() {
        return ResponseEntity.internalServerError().body(
                ExceptionDescription.builder()
                        .error("INTERNAL_SERVER_ERROR")
                        .message("An unexpected error occurred")
                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
