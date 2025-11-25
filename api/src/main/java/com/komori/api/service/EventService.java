package com.komori.api.service;

import com.komori.persistence.entity.EventEntity;
import com.komori.persistence.entity.UserEntity;
import com.komori.persistence.repository.EventRepository;
import com.komori.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final QueuePublisher queuePublisher;

    public String createEvent(String apiKey, String responseBody) {
        UserEntity user = userRepository.findByApiKey(apiKey);
        String eventId = UUID.randomUUID().toString();
        EventEntity event = EventEntity.builder()
                .eventId(eventId)
                .payload(responseBody)
                .user(user)
                .build();

        eventRepository.save(event);
        queuePublisher.publishEvent(eventId);
        return eventId;
    }
}
