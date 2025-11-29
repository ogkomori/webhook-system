package com.komori.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class JsonNodeConverter implements AttributeConverter<JsonNode, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting JsonNode to String", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(String s) {
        try {
            return objectMapper.readTree(s);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting String to JsonNode", e);
        }
    }
}
