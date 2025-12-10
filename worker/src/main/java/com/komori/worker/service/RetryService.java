package com.komori.worker.service;

import com.komori.persistence.dto.EventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Queue;

@Service
@RequiredArgsConstructor
public class RetryService {
    private final Queue<EventDTO> retryQueue;

    public void scheduleRetry(EventDTO event) {
        long delaySeconds = (long) Math.pow(2, event.getAttemptCount());
        event.setNextAttemptTime(Instant.now().plusSeconds(delaySeconds));
        retryQueue.add(event);
    }
}
