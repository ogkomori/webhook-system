package com.komori.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueuePublisher {
    private final RabbitTemplate rabbitTemplate;

    public void publishEvent(String eventId) {
        rabbitTemplate.convertAndSend("events", eventId);
    }
}
