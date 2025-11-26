package com.komori.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDTO {
    private String eventId;
    private String type;
    private String source;
    private Map<String, Object> payload;
    private Long timestamp;

    public boolean isValid() {
        return type != null && !type.isBlank()
                && source != null && !source.isBlank()
                && payload != null && !payload.isEmpty()
                && timestamp != null && timestamp > 0;
    }
}
