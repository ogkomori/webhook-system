package com.komori.worker.service;

import io.lettuce.core.api.sync.RedisCommands;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimiter {
    private final RedisCommands<String, String> redisCommands;

    public boolean isAllowed(String userId, int limitPerMinute) {
        try {
            String key = "rate:" + userId;
            Long count = redisCommands.incr(key);
            if (count == 1) {
                redisCommands.expire(key, 60);
            }

            return count <= limitPerMinute;
        } catch (Exception e) {
            return true;
        }
    }
}
