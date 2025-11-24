package com.komori.webhook.controller;

import com.komori.webhook.dto.RegistrationRequest;
import com.komori.webhook.dto.RegistrationResponse;
import com.komori.webhook.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/register")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ResponseEntity<RegistrationResponse> register(RegistrationRequest request) {
        RegistrationResponse response = userService.register(request);
        return ResponseEntity.ok(response);
    }
}
