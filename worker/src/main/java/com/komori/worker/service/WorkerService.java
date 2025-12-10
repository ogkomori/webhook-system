package com.komori.worker.service;

import com.komori.persistence.dto.EventDTO;
import com.komori.persistence.entity.EventEntity;
import com.komori.persistence.enumerated.EventStatus;
import com.komori.persistence.repository.EventRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Queue;

@Service
@RequiredArgsConstructor
public class WorkerService {
    private final WebhookDeliveryService webhookDeliveryService;
    private final EventRepository eventRepository;
    private final RateLimiter rateLimiter;
    private final Queue<EventDTO> retryQueue;
    private final RetryService retryService;

    public final int LIMIT_PER_MIN = 10;
    public final int MAX_ATTEMPTS = 5;

    public void processEvent(String eventId) {
        EventEntity eventEntity = eventRepository.findByEventId(eventId);
        if (eventEntity.getStatus() == EventStatus.DELIVERED) {
            return;
        }

        EventDTO event = new EventDTO(eventEntity);
        trySendingEvent(event);
    }

    public void trySendingEvent(EventDTO event) {
        event.setAttemptCount(event.getAttemptCount() + 1);

        if (!rateLimiter.isAllowed(event.getUserId(), LIMIT_PER_MIN)) {
            if (event.getAttemptCount() == MAX_ATTEMPTS) {
                webhookDeliveryService.markPermanentFailure(event.getEventId(), event.getAttemptCount(), "Too many concurrent requests");
            } else {
                retryService.scheduleRetry(event);
            }
        } else {
            webhookDeliveryService.deliver(event);
        }
    }

    @Scheduled(fixedDelay = 1000)
    public void processQueue() {
        Instant now = Instant.now();

        while (!retryQueue.isEmpty() && !retryQueue.peek().getNextAttemptTime().isAfter(now)) {
            EventDTO event = retryQueue.poll();
            trySendingEvent(event);
        }
    }
}
