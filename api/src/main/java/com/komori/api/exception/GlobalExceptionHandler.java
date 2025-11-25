package com.komori.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@SuppressWarnings("unused")
@Slf4j
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ExceptionDescription> handleMissingRequestBody() {
        return ResponseEntity.badRequest().body(
                ExceptionDescription.builder()
                        .error("MALFORMED_REQUEST_BODY")
                        .code(HttpStatus.BAD_REQUEST.value())
                        .message("Request body is missing or malformed")
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDescription> handleGeneralException(Exception e) {
        log.error("Unexpected error: {}", e.getMessage());
        return ResponseEntity.internalServerError().body(
                ExceptionDescription.builder()
                        .error("INTERNAL_SERVER_ERROR")
                        .message(e.getMessage()) // "An unexpected error occurred"
                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
