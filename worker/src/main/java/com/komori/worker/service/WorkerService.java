package com.komori.worker.service;

import com.komori.common.dto.EventDTO;
import com.komori.persistence.entity.DeliveryAttemptEntity;
import com.komori.persistence.entity.EventEntity;
import com.komori.persistence.enumerated.EventStatus;
import com.komori.persistence.repository.DeliveryAttemptRepository;
import com.komori.persistence.repository.EventRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class WorkerService {
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;

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

    private void deliver(String webhookUrl, EventDTO payload) {
        int attempt = 0;
        while (attempt < 5) {
            attempt++;

            try {
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.postForEntity(webhookUrl, payload, Void.class);

                deliveryAttemptRepository.save(DeliveryAttemptEntity.success(payload.getEventId(), attempt));
                eventRepository.updateEventStatus(payload.getEventId(), EventStatus.DELIVERED);
                return;
            } catch (Exception e) {
                deliveryAttemptRepository.save(DeliveryAttemptEntity.failure(payload.getEventId(), attempt, e.getMessage()));

                sleep(backoff(attempt));
            }
        }

        eventRepository.updateEventStatus(payload.getEventId(), EventStatus.FAILED);
        // TODO: Notify user of event delivery failure by email
    }

    private long backoff(int attempt) {
        return (long) (Math.pow(2, attempt) * 1000);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException _) {}
    }
}
