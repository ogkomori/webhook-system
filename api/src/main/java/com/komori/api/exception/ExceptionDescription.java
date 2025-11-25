package com.komori.api.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExceptionDescription {
    private String error;
    private Integer code;
    private LocalDateTime timestamp;
    private String message;
}
