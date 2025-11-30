package com.komori.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.komori.persistence.enumerated.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.sql.Timestamp;

@Entity
@Table(name = "events")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventId;
    @ManyToOne @JoinColumn(name = "user_id")
    private UserEntity user;
    @Enumerated(value = EnumType.STRING) @Builder.Default
    private EventStatus status = EventStatus.PENDING;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode payload;
    @CreationTimestamp
    private Timestamp createdAt;
}
