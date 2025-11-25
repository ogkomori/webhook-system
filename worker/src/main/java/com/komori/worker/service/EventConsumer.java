package com.komori.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EventConsumer {
    @RabbitListener(queues = "events")
    public void consume(String event) {
        // just to confirm consumption
        log.info("Event received: {}", event);
    }
}
