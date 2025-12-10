package com.komori.worker.config;

import com.komori.persistence.dto.EventDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

@Configuration
public class RetryQueueConfig {
    @Bean
    public Queue<EventDTO> retryQueue() {
        return new PriorityBlockingQueue<>(10, Comparator.comparing(EventDTO::getNextAttemptTime));
    }
}
