package com.komori.api.controller;

import com.komori.api.exception.InvalidApiKeyException;
import com.komori.api.exception.InvalidEventException;
import com.komori.api.service.EventService;
import com.komori.api.service.UserService;
import com.komori.common.dto.EventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final UserService userService;
    private final EventService eventService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveEvent(@RequestHeader(name = "X-API-KEY") String apiKey, @RequestBody(required = false) String body) {
        if (apiKey == null || apiKey.length() != 32 || !userService.isValidApiKey(apiKey)) {
            throw new InvalidApiKeyException();
        }

        if (body == null || body.isBlank()) {
            throw new InvalidEventException();
        }

        EventDTO eventDTO;
        try {
            eventDTO = objectMapper.readValue(body, EventDTO.class);
        } catch (JacksonException e) {
            throw new InvalidEventException();
        }

        if (!eventDTO.isValid()) {
            throw new InvalidEventException();
        }

        String eventId = eventService.createEvent(apiKey, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("success", true,
                        "eventId", eventId)
        );
    }
}
