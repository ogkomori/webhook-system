package com.komori.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.komori.persistence.dto.EventDTO;
import com.komori.persistence.entity.DeliveryAttemptEntity;
import com.komori.persistence.enumerated.EventStatus;
import com.komori.persistence.repository.DeliveryAttemptRepository;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Queue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        WebhookDeliveryService.class,
        WorkerService.class,
        RetryService.class,
        RetryQueueConfig.class
})
public class WebhookDeliveryServiceTests {
    @Autowired
    private WebhookDeliveryService webhookDeliveryService;
    @Autowired
    private WorkerService workerService;
    @Autowired
    private Queue<EventDTO> retryQueue;

    @MockitoBean
    private DeliveryAttemptRepository deliveryAttemptRepository;
    @MockitoBean
    private EventRepository eventRepository;
    @MockitoBean
    private RestTemplate restTemplate;
    @MockitoBean
    private RateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clearQueue() {
        retryQueue.clear();
    }

    @Test
    public void testSuccessfulDelivery() throws JsonProcessingException {
        EventDTO event = EventDTO.builder()
                .eventId("randomEvent")
                .userId("randomUser")
                .attemptCount(1)
                .nextAttemptTime(Instant.now())
                .webhookUrl("https://example.com")
                .payload(objectMapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        Mockito.when(restTemplate.postForEntity(Mockito.anyString(), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        Assertions.assertDoesNotThrow(() -> webhookDeliveryService.deliver(event));

        Mockito.verify(deliveryAttemptRepository).save(Mockito.argThat(DeliveryAttemptEntity::getSuccess));
        Mockito.verify(eventRepository).updateEventStatus(event.getEventId(), EventStatus.DELIVERED);
        Assertions.assertTrue(retryQueue.isEmpty());
    }

    @Test
    public void testSuccessAfterFailedAttempts() throws JsonProcessingException {
        EventDTO event = EventDTO.builder()
                .eventId("randomEvent")
                .userId("randomUser")
                .attemptCount(1)
                .nextAttemptTime(Instant.now())
                .webhookUrl("https://example.com")
                .payload(objectMapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        Mockito.when(restTemplate.postForEntity(Mockito.anyString(), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_GATEWAY))
                .thenReturn(new ResponseEntity<>(HttpStatus.OK));

        Assertions.assertDoesNotThrow(() -> webhookDeliveryService.deliver(event));

        Mockito.verify(deliveryAttemptRepository, Mockito.times(1)).save(Mockito.argThat(entity -> !entity.getSuccess()));
        Mockito.verifyNoInteractions(eventRepository);
        Assertions.assertEquals(1, retryQueue.size());

        Mockito.when(rateLimiter.isAllowed(event.getUserId(), workerService.LIMIT_PER_MIN))
                        .thenReturn(true);

        event.setNextAttemptTime(Instant.now());
        Assertions.assertDoesNotThrow(workerService::processQueue);

        Mockito.verify(deliveryAttemptRepository, Mockito.times(1)).save(Mockito.argThat(DeliveryAttemptEntity::getSuccess));
        Mockito.verify(eventRepository).updateEventStatus(event.getEventId(), EventStatus.DELIVERED);
        Assertions.assertTrue(retryQueue.isEmpty());
    }

    @Test
    public void testMaxRetriesFromClientResponse() throws JsonProcessingException {
        EventDTO event = EventDTO.builder()
                .eventId("randomEvent")
                .userId("randomUser")
                .attemptCount(webhookDeliveryService.MAX_ATTEMPTS)
                .nextAttemptTime(Instant.now())
                .webhookUrl("https://example.com")
                .payload(objectMapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        Mockito.when(restTemplate.postForEntity(Mockito.anyString(), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.BAD_GATEWAY));

        Assertions.assertDoesNotThrow(() -> webhookDeliveryService.deliver(event));

        Mockito.verify(deliveryAttemptRepository, Mockito.times(1)).save(Mockito.argThat(entity -> !entity.getSuccess()));
        Mockito.verify(eventRepository).updateEventStatus(event.getEventId(), EventStatus.FAILED);
        Assertions.assertTrue(retryQueue.isEmpty());
    }

    @Test
    public void testMaxRetriesFromException() throws JsonProcessingException {
        EventDTO event = EventDTO.builder()
                .eventId("randomEvent")
                .userId("randomUser")
                .attemptCount(webhookDeliveryService.MAX_ATTEMPTS)
                .nextAttemptTime(Instant.now())
                .webhookUrl("https://example.com")
                .payload(objectMapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        Mockito.when(restTemplate.postForEntity(Mockito.anyString(), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));

        Assertions.assertDoesNotThrow(() -> webhookDeliveryService.deliver(event));

        Mockito.verify(deliveryAttemptRepository, Mockito.times(1)).save(Mockito.argThat(entity -> !entity.getSuccess()));
        Mockito.verify(eventRepository).updateEventStatus(event.getEventId(), EventStatus.FAILED);
        Assertions.assertTrue(retryQueue.isEmpty());
    }

    @Test
    public void testPermanentFailureFromClientResponse() throws JsonProcessingException {
        EventDTO event = EventDTO.builder()
                .eventId("randomEvent")
                .userId("randomUser")
                .attemptCount(1)
                .nextAttemptTime(Instant.now())
                .webhookUrl("https://example.com")
                .payload(objectMapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        Mockito.when(restTemplate.postForEntity(Mockito.anyString(), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.FORBIDDEN));

        Assertions.assertDoesNotThrow(() -> webhookDeliveryService.deliver(event));

        Mockito.verify(deliveryAttemptRepository, Mockito.times(1)).save(Mockito.argThat(entity -> !entity.getSuccess()));
        Mockito.verify(eventRepository).updateEventStatus(event.getEventId(), EventStatus.FAILED);
        Assertions.assertTrue(retryQueue.isEmpty());
    }
}
