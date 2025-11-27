package com.komori.webhook;

import com.komori.common.dto.EventDTO;
import com.komori.persistence.entity.DeliveryAttemptEntity;
import com.komori.persistence.enumerated.EventStatus;
import com.komori.persistence.repository.DeliveryAttemptRepository;
import com.komori.persistence.repository.EventRepository;
import com.komori.worker.service.WorkerService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class WorkerServiceTests {
    @Mock
    EventRepository eventRepository;
    @Mock
    DeliveryAttemptRepository deliveryAttemptRepository;
    @Mock
    RestTemplate restTemplate;

    @Spy
    @InjectMocks
    WorkerService workerService;

    @Test
    public void testSuccessfulDelivery() {
        String webhookUrl = "https://example.com/events";
        EventDTO payload = EventDTO.builder()
                .eventId("randomEvent")
                .type("example")
                .payload(new HashMap<>())
                .timestamp(Instant.now().toEpochMilli())
                .source("https://allowedsource.com")
                .build();

        ResponseEntity<Void> okResponse = new ResponseEntity<>(HttpStatus.OK);
        Mockito.when(restTemplate.postForEntity(Mockito.eq(webhookUrl), Mockito.eq(payload), Mockito.eq(Void.class)))
                .thenReturn(okResponse);

        Assertions.assertDoesNotThrow(() -> workerService.deliver(webhookUrl, payload));

        Mockito.verify(deliveryAttemptRepository).save(Mockito.argThat(DeliveryAttemptEntity::getSuccess));
        Mockito.verify(deliveryAttemptRepository, Mockito.times(1)).save(Mockito.any(DeliveryAttemptEntity.class));
        Mockito.verify(eventRepository, Mockito.times(1)).updateEventStatus("randomEvent", EventStatus.DELIVERED);
    }

    @Test
    public void testMaxRetriesFromClientResponse() {
        String webhookUrl = "https://example.com/events";
        EventDTO payload = EventDTO.builder()
                .eventId("randomEvent")
                .type("example")
                .payload(new HashMap<>())
                .timestamp(Instant.now().toEpochMilli())
                .source("https://allowedsource.com")
                .build();

        ResponseEntity<Void> errorResponse = new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        Mockito.when(restTemplate.postForEntity(Mockito.eq(webhookUrl), Mockito.eq(payload), Mockito.eq(Void.class)))
                .thenReturn(errorResponse);
        Mockito.doNothing().when(workerService).sleep(Mockito.anyLong());

        Assertions.assertDoesNotThrow(() -> workerService.deliver(webhookUrl, payload));

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
    public void testMaxRetriesFromException() {
        String webhookUrl = "https://example.com/events";
        EventDTO payload = EventDTO.builder()
                .eventId("randomEvent")
                .type("example")
                .payload(new HashMap<>())
                .timestamp(Instant.now().toEpochMilli())
                .source("https://allowedsource.com")
                .build();

        Mockito.when(restTemplate.postForEntity(Mockito.eq(webhookUrl), Mockito.eq(payload), Mockito.eq(Void.class)))
                .thenThrow(new ResourceAccessException("Connection timout"));
        Mockito.doNothing().when(workerService).sleep(Mockito.anyLong());

        Assertions.assertDoesNotThrow(() -> workerService.deliver(webhookUrl, payload));

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
    public void testPermanentFailureFromClientResponseWithoutRetries() {
        String webhookUrl = "https://example.com/events";
        EventDTO payload = EventDTO.builder()
                .eventId("randomEvent")
                .type("example")
                .payload(new HashMap<>())
                .timestamp(Instant.now().toEpochMilli())
                .source("https://allowedsource.com")
                .build();

        ResponseEntity<Void> errorResponse = new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        Mockito.when(restTemplate.postForEntity(Mockito.eq(webhookUrl), Mockito.eq(payload), Mockito.eq(Void.class)))
                .thenReturn(errorResponse);

        Assertions.assertDoesNotThrow(() -> workerService.deliver(webhookUrl, payload));

        Mockito.verify(deliveryAttemptRepository).save(Mockito.argThat(entity -> !entity.getSuccess()));
        Mockito.verify(deliveryAttemptRepository, Mockito.times(1)).save(Mockito.any(DeliveryAttemptEntity.class));
        Mockito.verify(eventRepository, Mockito.times(1)).updateEventStatus("randomEvent", EventStatus.FAILED);
    }
}
