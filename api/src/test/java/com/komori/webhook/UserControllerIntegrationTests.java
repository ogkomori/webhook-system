package com.komori.webhook;

import com.komori.webhook.entity.UserEntity;
import com.komori.webhook.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class UserControllerIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @SuppressWarnings("resource")
    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:latest")
            .withDatabaseName("db")
            .withUsername("user")
            .withPassword("pass");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Test
    void registerDuplicateEmail() throws Exception {
        userRepository.save(UserEntity.builder()
                .email("test@example.com")
                .build());

        String requestJson = "{\"email\":\"test@example.com\", \"webhookUrl\":\"https://example.com\"}";

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isConflict());
    }

    @Test
    void registerInvalidEmail() throws Exception {
        String requestJson = "{\"email\":\"bad_email\", \"webhookUrl\":\"https://example.com\"}";

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerInvalidUrl() throws Exception {
        String requestJson = "{\"email\":\"email@example.com\", \"webhookUrl\":\"hts://invalid.url\"}";

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/users/register")).andExpect(status().isBadRequest());
    }

    @Test
    void completeRegistration() throws Exception {
        String requestJson = "{\"email\":\"test@example.com\", \"webhookUrl\":\"https://example.com\"}";

        mockMvc.perform(post("/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isCreated());

        Assertions.assertTrue(userRepository.existsByEmail("test@example.com"));
    }
}
