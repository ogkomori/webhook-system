package com.komori.worker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.komori.persistence.dto.EventDTO;
import com.komori.persistence.entity.DeliveryAttemptEntity;
import com.komori.persistence.enumerated.EventStatus;
import com.komori.persistence.repository.DeliveryAttemptRepository;
import com.komori.persistence.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class WebhookDeliveryService {
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final EventRepository eventRepository;
    private final RestTemplate restTemplate;
    private final RetryService retryService;

    public final int MAX_ATTEMPTS = 5;

    public void deliver(EventDTO event) {
        String eventId = event.getEventId();
        JsonNode payload = event.getPayload();
        String webhookUrl = event.getWebhookUrl();
        int attempt = event.getAttemptCount();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Event-Id", eventId);
            HttpEntity<JsonNode> httpEntity = new HttpEntity<>(payload, headers);
            ResponseEntity<Void> response = restTemplate.postForEntity(webhookUrl, httpEntity, Void.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                markSuccessfulAttempt(eventId, attempt);
            }
            else if (response.getStatusCode().is4xxClientError()) {
                markPermanentFailure(eventId, attempt, "Received client error code " + response.getStatusCode().value());
            }
            else {
                if (attempt == MAX_ATTEMPTS) {
                    markPermanentFailure(eventId, attempt, "Maximum number of retries attempted");
                } else {
                    markFailedAttempt(eventId, attempt, "Received client error code " + response.getStatusCode().value());
                    retryService.scheduleRetry(event);
                }
            }
        } catch (Exception e) {
            if (attempt == MAX_ATTEMPTS) {
                markPermanentFailure(eventId, attempt, "Maximum number of retries attempted");
            } else {
                markFailedAttempt(eventId, attempt, e.getMessage());
                retryService.scheduleRetry(event);
            }
        }

        // TODO: Notify user of event delivery failure by email
    }

    public void markSuccessfulAttempt(String eventId, int attempt) {
        deliveryAttemptRepository.save(DeliveryAttemptEntity.success(eventId, attempt));
        eventRepository.updateEventStatus(eventId, EventStatus.DELIVERED);
    }

    public void markFailedAttempt(String eventId, int attempt, String errorMsg) {
        deliveryAttemptRepository.save(DeliveryAttemptEntity.failure(eventId, attempt, "Error delivering webhook on attempt " + attempt + ": " + errorMsg));
    }

    public void markPermanentFailure(String eventId, int attempt, String errorMsg) {
        deliveryAttemptRepository.save(DeliveryAttemptEntity.failure(eventId, attempt, "Permanent error delivering webhook on attempt " + attempt + ": " + errorMsg));
        eventRepository.updateEventStatus(eventId, EventStatus.FAILED);
    }
}
