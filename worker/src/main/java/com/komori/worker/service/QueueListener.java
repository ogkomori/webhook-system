package com.komori.worker.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueListener {
    private final WorkerService workerService;

    @RabbitListener(queues = "events")
    public void consume(String eventId) {
        // just to confirm consumption
        log.info("Event received: {}", eventId);
        workerService.processEvent(eventId);
    }
}
