package com.komori.webhook.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProducer {
    private final RabbitTemplate rabbitTemplate;
    private static int num = 1;

    @Scheduled(fixedRate = 10000)
    public void publishEvent() {
        String event = "Event Number " + num;
        rabbitTemplate.convertAndSend("events", event);
        log.info("Event published: {}", event);
        num++;
    }
}
