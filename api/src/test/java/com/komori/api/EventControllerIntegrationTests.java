package com.komori.api;

import com.komori.api.controller.EventController;
import com.komori.api.service.EventService;
import com.komori.api.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = EventController.class)
public class EventControllerIntegrationTests {
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private EventService eventService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testMissingApiKey() throws Exception {
        String payload = "{\"event\":\"something happened\"}";

        mockMvc.perform(post("/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MISSING_API_KEY"));
    }

    @Test
    public void testInvalidApiKey_EmptyString() throws Exception {
        String payload = "{\n\"event\":\"something happened\"\n}";

        mockMvc.perform(post("/events")
                        .header("X-API-KEY", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    public void testInvalidApiKey_IncorrectKey() throws Exception {
        String payload = "{\n\"event\":\"something happened\"\n}";
        String apiKey = "invalidApiKey";

        Mockito.when(userService.isValidApiKey(apiKey)).thenReturn(false);

        mockMvc.perform(post("/events")
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    public void testInvalidEvent_NotJSON() throws Exception {
        String payload = "[1,2,3]";
        String apiKey = "validApiKey";

        Mockito.when(userService.isValidApiKey(apiKey)).thenReturn(true);

        mockMvc.perform(post("/events")
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"));
    }

    @Test
    public void testInvalidEvent_EmptyJson() throws Exception {
        String payload = "{}";
        String apiKey = "validApiKey";

        Mockito.when(userService.isValidApiKey(apiKey)).thenReturn(true);

        mockMvc.perform(post("/events")
                        .header("X-API-KEY", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"));
    }
}
