package com.komori.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userId;
    private String email;
    private String apiKey;
    private String webhookUrl;
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE) @JsonIgnore @Builder.Default
    private List<EventEntity> events = new ArrayList<>();
    @CreationTimestamp
    private Timestamp createdAt;
}
