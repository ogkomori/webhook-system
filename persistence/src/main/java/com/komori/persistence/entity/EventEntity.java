package com.komori.persistence.entity;

import com.komori.persistence.enumerated.EventStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Entity
@Table(name = "events")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventId;
    @ManyToOne @JoinColumn(name = "user_id")
    private UserEntity user;
    @Enumerated(value = EnumType.STRING) @Builder.Default
    private EventStatus status = EventStatus.PENDING;
    private String payload;
    @CreationTimestamp
    private Timestamp createdAt;
}
