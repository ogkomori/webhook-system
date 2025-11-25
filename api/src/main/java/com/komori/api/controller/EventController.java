package com.komori.api.controller;

import com.komori.api.exception.InvalidApiKeyException;
import com.komori.api.service.EventService;
import com.komori.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final UserService userService;
    private final EventService eventService;

    @PostMapping
    public ResponseEntity<?> receiveEvent(@RequestHeader(name = "X-API-KEY") String apiKey, @RequestBody(required = false) String body) {
        if (apiKey == null || apiKey.length() != 32 || !userService.isValidApiKey(apiKey)) {
            throw new InvalidApiKeyException();
        }

        // TODO: validate requestBody

        String eventId = eventService.createEvent(apiKey, body);
        return ResponseEntity.status(HttpStatus.CREATED).body("Created event " + eventId);
    }
}
