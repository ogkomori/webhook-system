package com.komori.api.service;

import com.komori.persistence.repository.UserRepository;
import com.komori.api.dto.RegistrationRequest;
import com.komori.api.dto.RegistrationResponse;
import com.komori.persistence.entity.UserEntity;
import com.komori.api.exception.EmailAlreadyExistsException;
import com.komori.api.exception.InvalidEmailException;
import com.komori.api.exception.InvalidUrlException;
import com.komori.api.exception.MissingEmailOrUrlException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public RegistrationResponse register(RegistrationRequest request) {
        if (request == null) {
            throw new MissingEmailOrUrlException();
        }

        if (!EmailValidator.getInstance().isValid(request.getEmail())) {
            throw new InvalidEmailException();
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        if (!UrlValidator.getInstance().isValid(request.getWebhookUrl())) {
            throw new InvalidUrlException();
        }

        String apiKey = generateApiKey();
        while (userRepository.existsByApiKey(apiKey)) {
            apiKey = generateApiKey();
        }

        String uuid = UUID.randomUUID().toString();
        while (userRepository.existsByUserId(uuid)) {
            uuid = UUID.randomUUID().toString();
        }

        UserEntity newUser = UserEntity.builder()
                .email(request.getEmail())
                .webhookUrl(request.getWebhookUrl())
                .apiKey(apiKey)
                .userId(uuid)
                .build();

        userRepository.save(newUser);

        return new RegistrationResponse(uuid, apiKey);
    }

    public boolean isValidApiKey(String apiKey) {
        return userRepository.existsByApiKey(apiKey);
    }

    private String generateApiKey() {
        String alphabet = "ABCDEFGHIJKLMNOPQRTSUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        SecureRandom random = new SecureRandom();
        char[] key = new char[32];
        for (int i = 0; i < 32; i++) {
            int j = random.nextInt(alphabet.length());
            key[i] = alphabet.charAt(j);
        }

        return new String(key);
    }
}
