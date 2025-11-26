package com.komori.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "delivery_attempts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeliveryAttemptEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventId;
    private Integer attempt;
    private Boolean success;
    private String error;
    private Instant timestamp;

    public static DeliveryAttemptEntity success(String eventId, int attempt) {
        return DeliveryAttemptEntity.builder()
                .attempt(attempt)
                .eventId(eventId)
                .success(true)
                .timestamp(Instant.now())
                .build();
    }

    public static DeliveryAttemptEntity failure(String eventId, int attempt, String errorMsg) {
        return DeliveryAttemptEntity.builder()
                .attempt(attempt)
                .eventId(eventId)
                .success(false)
                .error(errorMsg)
                .timestamp(Instant.now())
                .build();
    }
}
