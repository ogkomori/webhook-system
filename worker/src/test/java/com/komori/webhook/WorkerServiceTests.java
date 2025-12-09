package com.komori.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.komori.persistence.entity.DeliveryAttemptEntity;
import com.komori.persistence.entity.EventEntity;
import com.komori.persistence.entity.UserEntity;
import com.komori.persistence.enumerated.EventStatus;
import com.komori.persistence.repository.DeliveryAttemptRepository;
import com.komori.persistence.repository.EventRepository;
import com.komori.worker.service.RateLimiter;
import com.komori.worker.service.WorkerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class WorkerServiceTests {
    @Mock
    EventRepository eventRepository;
    @Mock
    DeliveryAttemptRepository deliveryAttemptRepository;
    @Mock
    RestTemplate restTemplate;
    @Mock
    RateLimiter rateLimiter;
    @Mock
    ObjectMapper mapper;

    @Spy
    @InjectMocks
    WorkerService workerService;

    @Test
    public void testSuccessfulDelivery() throws JsonProcessingException {
        EventEntity event = EventEntity.builder()
                .user(new UserEntity(1L, "user", "email", "apiKey", "https://example.com/events", new ArrayList<>(), Timestamp.from(Instant.now())))
                .eventId("randomEvent")
                .payload(mapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        ResponseEntity<Void> okResponse = new ResponseEntity<>(HttpStatus.OK);
        Mockito.when(restTemplate.postForEntity(Mockito.eq(event.getUser().getWebhookUrl()), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenReturn(okResponse);
        Mockito.when(rateLimiter.isAllowed(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(true);

        Assertions.assertDoesNotThrow(() -> workerService.deliver(event));

        Mockito.verify(deliveryAttemptRepository).save(Mockito.argThat(DeliveryAttemptEntity::getSuccess));
        Mockito.verify(deliveryAttemptRepository, Mockito.times(1)).save(Mockito.any(DeliveryAttemptEntity.class));
        Mockito.verify(eventRepository, Mockito.times(1)).updateEventStatus("randomEvent", EventStatus.DELIVERED);
    }

    @Test
    public void testRateLimitedDelivery() throws JsonProcessingException {
        EventEntity event = EventEntity.builder()
                .user(new UserEntity(1L, "user", "email", "apiKey", "https://example.com/events", new ArrayList<>(), Timestamp.from(Instant.now())))
                .eventId("randomEvent")
                .payload(mapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        Mockito.doNothing().when(workerService).sleep(Mockito.anyLong());
        Mockito.when(rateLimiter.isAllowed(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);
        ResponseEntity<Void> okResponse = new ResponseEntity<>(HttpStatus.OK);
        Mockito.when(restTemplate.postForEntity(Mockito.eq(event.getUser().getWebhookUrl()), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenReturn(okResponse);

        Assertions.assertDoesNotThrow(() -> workerService.deliver(event));

        Mockito.verify(workerService, Mockito.times(3)).sleep(60000);
    }

    @Test
    public void testMaxRetriesFromClientResponse() throws JsonProcessingException {
        EventEntity event = EventEntity.builder()
                .user(new UserEntity(1L, "user", "email", "apiKey", "https://example.com/events", new ArrayList<>(), Timestamp.from(Instant.now())))
                .eventId("randomEvent")
                .payload(mapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        ResponseEntity<Void> errorResponse = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        Mockito.when(restTemplate.postForEntity(Mockito.eq(event.getUser().getWebhookUrl()), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenReturn(errorResponse);
        Mockito.doNothing().when(workerService).sleep(Mockito.anyLong());
        Mockito.when(rateLimiter.isAllowed(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(true);

        Assertions.assertDoesNotThrow(() -> workerService.deliver(event));

        ArgumentCaptor<DeliveryAttemptEntity> argumentCaptor = ArgumentCaptor.forClass(DeliveryAttemptEntity.class);
        Mockito.verify(deliveryAttemptRepository, Mockito.times(5)).save(argumentCaptor.capture());
        List<DeliveryAttemptEntity> entities = argumentCaptor.getAllValues();
        for (DeliveryAttemptEntity entity : entities) {
            Assertions.assertFalse(entity.getSuccess());
        }
        Mockito.verify(workerService, Mockito.times(4)).sleep(Mockito.anyLong());
        Mockito.verify(eventRepository, Mockito.times(1)).updateEventStatus("randomEvent", EventStatus.FAILED);
    }

    @Test
    public void testMaxRetriesFromException() throws JsonProcessingException {
        EventEntity event = EventEntity.builder()
                .user(new UserEntity(1L, "user", "email", "apiKey", "https://example.com/events", new ArrayList<>(), Timestamp.from(Instant.now())))
                .eventId("randomEvent")
                .payload(mapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        Mockito.when(restTemplate.postForEntity(Mockito.eq(event.getUser().getWebhookUrl()), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection timeout"));
        Mockito.doNothing().when(workerService).sleep(Mockito.anyLong());
        Mockito.when(rateLimiter.isAllowed(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(true);

        Assertions.assertDoesNotThrow(() -> workerService.deliver(event));

        ArgumentCaptor<DeliveryAttemptEntity> argumentCaptor = ArgumentCaptor.forClass(DeliveryAttemptEntity.class);
        Mockito.verify(deliveryAttemptRepository, Mockito.times(5)).save(argumentCaptor.capture());
        List<DeliveryAttemptEntity> entities = argumentCaptor.getAllValues();
        for (DeliveryAttemptEntity entity : entities) {
            Assertions.assertFalse(entity.getSuccess());
        }
        Mockito.verify(workerService, Mockito.times(4)).sleep(Mockito.anyLong());
        Mockito.verify(eventRepository, Mockito.times(1)).updateEventStatus("randomEvent", EventStatus.FAILED);
    }

    @Test
    public void testPermanentFailureFromClientResponseWithoutRetries() throws JsonProcessingException {
        EventEntity event = EventEntity.builder()
                .user(new UserEntity(1L, "user", "email", "apiKey", "https://example.com/events", new ArrayList<>(), Timestamp.from(Instant.now())))
                .eventId("randomEvent")
                .payload(mapper.readTree("{\"event\":\"something happened\"}"))
                .build();

        ResponseEntity<Void> errorResponse = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        Mockito.when(restTemplate.postForEntity(Mockito.eq(event.getUser().getWebhookUrl()), Mockito.any(HttpEntity.class), Mockito.eq(Void.class)))
                .thenReturn(errorResponse);
        Mockito.when(rateLimiter.isAllowed(Mockito.anyString(), Mockito.anyInt()))
                .thenReturn(true);

        Assertions.assertDoesNotThrow(() -> workerService.deliver(event));

        Mockito.verify(deliveryAttemptRepository).save(Mockito.argThat(entity -> !entity.getSuccess()));
        Mockito.verify(deliveryAttemptRepository, Mockito.times(1)).save(Mockito.any(DeliveryAttemptEntity.class));
        Mockito.verify(eventRepository, Mockito.times(1)).updateEventStatus("randomEvent", EventStatus.FAILED);
    }
}
