package com.komori.api;

import com.komori.persistence.entity.UserEntity;
import com.komori.persistence.repository.UserRepository;
import com.komori.api.dto.RegistrationRequest;
import com.komori.api.exception.EmailAlreadyExistsException;
import com.komori.api.exception.InvalidEmailException;
import com.komori.api.exception.InvalidUrlException;
import com.komori.api.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {
    @Mock
    UserRepository userRepository;
    @InjectMocks
    UserService userService;

    @Test
    void emailAlreadyExists() {
        RegistrationRequest request = new RegistrationRequest("test@example.com", "https://example.com");
        Mockito.when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);
        Assertions.assertThrows(EmailAlreadyExistsException.class, () -> userService.register(request));
    }

    @Test
    void invalidEmail1() {
        RegistrationRequest request = new RegistrationRequest("email", "https://example.com");
        Assertions.assertThrows(InvalidEmailException.class, () -> userService.register(request));
    }

    @Test
    void invalidEmail2() {
        RegistrationRequest request = new RegistrationRequest("email@domain", "https://example.com");
        Assertions.assertThrows(InvalidEmailException.class, () -> userService.register(request));
    }

    @Test
    void invalidUrl1() {
        RegistrationRequest request = new RegistrationRequest("email@domain.com", "url");
        Assertions.assertThrows(InvalidUrlException.class, () -> userService.register(request));
    }

    @Test
    void invalidUrl2() {
        RegistrationRequest request = new RegistrationRequest("email@domain.com", "htt://url.com");
        Assertions.assertThrows(InvalidUrlException.class, () -> userService.register(request));
    }

    @Test
    void completeRegistration() {
        RegistrationRequest request = new RegistrationRequest("email@domain.com", "https://example.com");

        Assertions.assertDoesNotThrow(() -> userService.register(request));

        Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any(UserEntity.class));
        Mockito.verify(userRepository).save(Mockito.argThat(userEntity -> userEntity.getUserId() != null && userEntity.getApiKey() != null));
    }
}
