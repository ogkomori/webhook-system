package com.komori.worker.service;

import com.komori.common.dto.EventDTO;
import com.komori.persistence.entity.DeliveryAttemptEntity;
import com.komori.persistence.entity.EventEntity;
import com.komori.persistence.enumerated.EventStatus;
import com.komori.persistence.repository.DeliveryAttemptRepository;
import com.komori.persistence.repository.EventRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class WorkerService {
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public void process(String eventId) {
        EventEntity event = eventRepository.findByEventId(eventId);
        if (event.getStatus() == EventStatus.DELIVERED) {
            return;
        }

        String payload = event.getPayload();
        EventDTO eventDTO = objectMapper.readValue(payload, EventDTO.class);
        eventDTO.setEventId(eventId);
        String webhookUrl = event.getUser().getWebhookUrl();
        deliver(webhookUrl, eventDTO);
    }

    public void deliver(String webhookUrl, EventDTO payload) {
        int attempt = 0;
        while (attempt < 5) {
            attempt++;

            try {
                ResponseEntity<Void> response = restTemplate.postForEntity(webhookUrl, payload, Void.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    markSuccessfulAttempt(payload.getEventId(), attempt);
                    return;
                } else if (response.getStatusCode().is4xxClientError()) {
                    markPermanentFailure(payload.getEventId(), attempt, "Received client error code " + response.getStatusCode().value());
                    return;
                } else {
                    if (attempt == 5) {
                        markPermanentFailure(payload.getEventId(), attempt, "Maximum number of retries attempted");
                        return;
                    }
                    markFailedAttempt(payload.getEventId(), attempt, "Received client error code " + response.getStatusCode().value());
                    sleep(backoff(attempt));
                }
            } catch (Exception e) {
                if (attempt == 5) {
                    markPermanentFailure(payload.getEventId(), attempt, "Maximum number of retries attempted");
                    return;
                }
                markFailedAttempt(payload.getEventId(), attempt, e.getMessage());
                sleep(backoff(attempt));
            }
        }
        // TODO: Notify user of event delivery failure by email
    }

    private void markSuccessfulAttempt(String eventId, int attempt) {
        deliveryAttemptRepository.save(DeliveryAttemptEntity.success(eventId, attempt));
        eventRepository.updateEventStatus(eventId, EventStatus.DELIVERED);
    }

    private void markFailedAttempt(String eventId, int attempt, String errorMsg) {
        deliveryAttemptRepository.save(DeliveryAttemptEntity.failure(eventId, attempt, "Error delivering webhook on attempt " + attempt + ": " + errorMsg));
    }

    private void markPermanentFailure(String eventId, int attempt, String errorMsg) {
        deliveryAttemptRepository.save(DeliveryAttemptEntity.failure(eventId, attempt, "Permanent error delivering webhook on attempt " + attempt + ": " + errorMsg));
        eventRepository.updateEventStatus(eventId, EventStatus.FAILED);
    }

    private long backoff(int attempt) {
        return (long) (Math.pow(2, attempt) * 1000);
    }

    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException _) {}
    }
}
