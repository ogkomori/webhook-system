package com.komori.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.komori.api.exception.InvalidApiKeyException;
import com.komori.api.service.EventService;
import com.komori.api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {
    private final UserService userService;
    private final EventService eventService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveEvent(@RequestHeader(name = "X-API-KEY") String apiKey, @RequestBody(required = false) Map<String,Object> body) {
        if (apiKey == null || apiKey.isBlank() || !userService.isValidApiKey(apiKey)) {
            throw new InvalidApiKeyException();
        }

        if (body.isEmpty()) {
            throw new HttpMessageNotReadableException("", new MockHttpInputMessage(new byte[0]));
        }

        JsonNode jsonNode = objectMapper.valueToTree(body);
        String eventId = eventService.createEvent(apiKey, jsonNode);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                Map.of("success", true,
                        "eventId", eventId)
        );
    }
}
