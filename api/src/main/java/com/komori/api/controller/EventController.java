package com.komori.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.komori.api.exception.InvalidApiKeyException;
import com.komori.api.exception.InvalidEventException;
import com.komori.api.exception.MissingApiKeyException;
import com.komori.api.service.EventService;
import com.komori.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final UserService userService;
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveEvent(@RequestHeader(name = "X-API-KEY") String apiKey, @RequestBody JsonNode body) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new MissingApiKeyException();
        }

        if (!userService.isValidApiKey(apiKey)) {
            throw new InvalidApiKeyException();
        }

        if (!body.isObject()) {
            throw new InvalidEventException();
        }

        String eventId = eventService.createEvent(apiKey, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("success", true,
                        "eventId", eventId)
        );
    }
}
