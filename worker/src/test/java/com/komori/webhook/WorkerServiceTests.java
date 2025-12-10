package com.komori.webhook;

import com.komori.persistence.dto.EventDTO;
import com.komori.persistence.repository.EventRepository;
import com.komori.worker.config.RetryQueueConfig;
import com.komori.worker.service.RateLimiter;
import com.komori.worker.service.RetryService;
import com.komori.worker.service.WebhookDeliveryService;
import com.komori.worker.service.WorkerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        WorkerService.class,
        RetryService.class,
        RetryQueueConfig.class
})
public class WorkerServiceTests {
    @Autowired
    private WorkerService workerService;
    @Autowired
    private Queue<EventDTO> retryQueue;

    @MockitoBean
    private WebhookDeliveryService webhookDeliveryService;
    @MockitoBean
    private RateLimiter rateLimiter;
    @MockitoBean
    private EventRepository eventRepository;


    @BeforeEach
    void clearQueue() {
        retryQueue.clear();
    }

    @Test
    public void testSuccessfulDelivery() {
        EventDTO event = EventDTO.builder()
                .eventId("randomEvent")
                .userId("randomUser")
                .attemptCount(0)
                .nextAttemptTime(Instant.now())
                .webhookUrl("https://example.com")
                .build();

        Mockito.when(rateLimiter.isAllowed(event.getUserId(), workerService.LIMIT_PER_MIN))
                        .thenReturn(true);

        Assertions.assertDoesNotThrow(() -> workerService.trySendingEvent(event));

        Assertions.assertTrue(retryQueue.isEmpty());
        Mockito.verify(webhookDeliveryService, Mockito.times(1)).deliver(event);
    }

    @Test
    public void testRateLimitedSuccessfulDelivery() {
        EventDTO event = EventDTO.builder()
                .eventId("randomEvent")
                .userId("randomUser")
                .attemptCount(0)
                .nextAttemptTime(Instant.now())
                .webhookUrl("https://example.com")
                .build();

        Mockito.when(rateLimiter.isAllowed(event.getUserId(), workerService.LIMIT_PER_MIN))
                .thenReturn(false)
                .thenReturn(true);

        Assertions.assertDoesNotThrow(() -> workerService.trySendingEvent(event));

        Assertions.assertEquals(1, retryQueue.size());
        Mockito.verifyNoInteractions(webhookDeliveryService);

        event.setNextAttemptTime(Instant.now());
        Assertions.assertDoesNotThrow(workerService::processQueue);

        Assertions.assertTrue(retryQueue.isEmpty());
        Mockito.verify(webhookDeliveryService, Mockito.times(1)).deliver(event);
    }

    @Test
    public void testMaxRateLimitedAttempts() {
        EventDTO event = EventDTO.builder()
                .eventId("randomEvent")
                .userId("randomUser")
                .attemptCount(workerService.MAX_ATTEMPTS - 1)
                .nextAttemptTime(Instant.now())
                .webhookUrl("https://example.com")
                .build();

        Mockito.when(rateLimiter.isAllowed(event.getUserId(), workerService.LIMIT_PER_MIN))
                .thenReturn(false);

        Assertions.assertDoesNotThrow(() -> workerService.trySendingEvent(event));

        Assertions.assertTrue(retryQueue.isEmpty());
        Mockito.verify(webhookDeliveryService, Mockito.times(1)).markPermanentFailure(event.getEventId(), event.getAttemptCount(), "Too many concurrent requests");
    }

    @Test
    public void testQueuePreservesOrder() {
        EventDTO event1 = EventDTO.builder()
                .eventId("randomEvent")
                .userId("randomUser")
                .attemptCount(0)
                .nextAttemptTime(Instant.now())
                .webhookUrl("https://example.com")
                .build();

        EventDTO event2 = EventDTO.builder()
                .eventId("randomEvent2")
                .userId("randomUser")
                .attemptCount(0)
                .nextAttemptTime(Instant.now().plusMillis(10))
                .webhookUrl("https://example.com")
                .build();

        EventDTO event3 = EventDTO.builder()
                .eventId("randomEvent3")
                .userId("randomUser")
                .attemptCount(0)
                .nextAttemptTime(Instant.now().plusMillis(20))
                .webhookUrl("https://example.com")
                .build();

        Mockito.when(rateLimiter.isAllowed(Mockito.anyString(), Mockito.eq(workerService.LIMIT_PER_MIN)))
                .thenReturn(false);

        Assertions.assertDoesNotThrow(() -> {
            workerService.trySendingEvent(event3);
            workerService.trySendingEvent(event1);
            workerService.trySendingEvent(event2);
        });

        Assertions.assertEquals(3, retryQueue.size());

        List<EventDTO> events = new ArrayList<>(retryQueue);
        List<Instant> times = events.stream()
                .map(EventDTO::getNextAttemptTime)
                .toList();

        List<Instant> sorted = new ArrayList<>(times);
        Collections.sort(sorted);

        Assertions.assertEquals(sorted, times);
    }
}
