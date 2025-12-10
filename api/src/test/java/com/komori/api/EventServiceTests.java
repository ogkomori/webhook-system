package com.komori.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.komori.api.service.EventService;
import com.komori.api.service.QueuePublisher;
import com.komori.persistence.entity.EventEntity;
import com.komori.persistence.entity.UserEntity;
import com.komori.persistence.repository.EventRepository;
import com.komori.persistence.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class EventServiceTests {
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private QueuePublisher queuePublisher;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventService eventService;

    @Test
    public void testEventCreation() throws JsonProcessingException {
        String apiKey = "validApiKey";
        JsonNode payload = objectMapper.readTree("{\"event\":\"something happened\"}");
        UserEntity user = UserEntity.builder()
                .id(1L)
                .apiKey(apiKey)
                .events(new ArrayList<>())
                .userId(UUID.randomUUID().toString())
                .webhookUrl("https://example.com")
                .build();

        Mockito.when(userRepository.findByApiKey(apiKey)).thenReturn(user);

        Assertions.assertDoesNotThrow(() -> eventService.createEvent(apiKey, payload));

        Mockito.verify(eventRepository, Mockito.times(1)).save(Mockito.any(EventEntity.class));
        Mockito.verify(queuePublisher, Mockito.times(1)).publishEvent(Mockito.anyString());
    }
}
