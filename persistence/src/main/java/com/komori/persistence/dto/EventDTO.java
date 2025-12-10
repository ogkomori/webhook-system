package com.komori.persistence.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.komori.persistence.entity.EventEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {
    private String eventId;
    private String userId;
    private String webhookUrl;
    private JsonNode payload;
    private int attemptCount;
    private Instant nextAttemptTime;

    public EventDTO(EventEntity eventEntity) {
        this.eventId = eventEntity.getEventId();
        this.userId = eventEntity.getUser().getUserId();
        this.webhookUrl = eventEntity.getUser().getWebhookUrl();
        this.payload = eventEntity.getPayload();
        this.attemptCount = 0;
        this.nextAttemptTime = Instant.now();
    }
}
